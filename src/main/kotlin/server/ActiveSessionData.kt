package server

import db.CharacterInfo
import db.DBOperator
import db.SessionInfo

import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

const val initPrevCharacterMovedId: Int = -1

class ActiveSessionData(
    val sessionId: UInt,
    val mapId: UInt,
    val started: Instant,
    var moveProperties: MoveProperties = MoveProperties(),
    val activeUsers: MutableMap<UInt, UserData> = Collections.synchronizedMap(mutableMapOf())
) {
    data class UserData(
        val userId: UInt,
        val sessionId: UInt,
        val connections: MutableSet<Connection> = Collections.synchronizedSet(mutableSetOf()),
        var characters: MutableSet<UInt> = Collections.synchronizedSet(mutableSetOf())
    )

    data class MoveProperties(
        var prevCharacterMovedId: AtomicInteger = AtomicInteger(initPrevCharacterMovedId)
    )

    constructor(sessionInfo: SessionInfo): this(
        sessionInfo.id,
        sessionInfo.mapID,
        sessionInfo.started,
        MoveProperties(AtomicInteger(sessionInfo.whoCanMove))
    )

    fun toJson(): String {
        val json = JSONObject()
            .put("sessionId", sessionId)
            .put("mapId", mapId)
            .put("started", started)
        return json.toString()
    }

    fun toSessionInfo(): SessionInfo {
        return SessionInfo(
            sessionId,
            mapId,
            true,
            started,
            moveProperties.prevCharacterMovedId.get()
        )
    }

    suspend fun startConnection(userId: UInt, connection: Connection) {
        sendSafety(connection.connection, toJson())

        val isFirstConnectionForThisUser = !activeUsers.containsKey(userId)
        if (isFirstConnectionForThisUser) {
            activeUsers[userId] = UserData(userId, sessionId)
        }

        val userData = activeUsers.getValue(userId)
        userData.connections.add(connection)

        activeUsers.forEach {
            val own = (it.key == userId)
            it.value.characters.forEach { characterId ->
                val character = DBOperator.getCharacterByID(characterId)
                    ?: throw Exception("Character with ID $characterId does not exist")
                showCharacter(character, connection, own)
            }
        }
        logger.info("Session #$sessionId for user #$userId: show all active characters")

        if (isFirstConnectionForThisUser) {
            DBOperator.getAllCharactersOfUserInSession(userId, sessionId).forEach {
                addCharacter(it)
            }
        } else {
            userData.characters.forEach {
                val curCharacterForMoveId = getCurrentCharacterForMoveId()
                if (it == curCharacterForMoveId) {
                    sendCharacterStatusToConn(it, true, connection)
                }
            }
        }
    }

    suspend fun finishConnection(userId: UInt, connection: Connection) {
        val userData = activeUsers.getValue(userId)
        userData.connections.remove(connection)

        if (userData.connections.isEmpty()) {
            userData.characters.forEach {
                val character = DBOperator.getCharacterByID(it)
                    ?: throw Exception("Character with ID $it does not exist")
                removeCharacter(character)
            }
            activeUsers.remove(userId)
        }
    }

    private suspend fun showCharacter(character: CharacterInfo, connection: Connection, own: Boolean) {
        val characterJson = JSONObject(Json.encodeToString(character))
            .put("type", "character:new")
            .put("own", own)
        sendSafety(connection.connection, characterJson.toString())
    }

    fun getValidCharacter(message: JSONObject, userId: UInt): CharacterInfo {
        val characterId = message.getInt("id").toUInt()
        val character = DBOperator.getCharacterByID(characterId)
            ?: throw Exception("Character with ID $characterId does not exist")
        if (character.userId != userId)
            throw Exception("Character with ID $characterId doesn't belong to you")
        if (character.sessionId != sessionId)
            throw Exception("Character with ID $characterId doesn't belong to this game session")
        return character
    }

    fun getValidOpponentCharacter(message: JSONObject): CharacterInfo {
        val characterId = message.getInt("opponentId").toUInt()
        val character = DBOperator.getCharacterByID(characterId)
            ?: throw Exception("Opponent character with ID $characterId does not exist")
        if (character.sessionId != sessionId)
            throw Exception("Opponent character with ID $characterId doesn't belong to this game session")
        return character
    }

    suspend fun addCharacter(character: CharacterInfo) {
        val characterIdForMoveBeforeAdding = getCurrentCharacterForMoveId()

        val userData = activeUsers.getValue(character.userId)
        userData.characters.add(character.id)

        val message = JSONObject(Json.encodeToString(character))
            .put("type", "character:new")
            .put("own", true)
        userData.connections.forEach {
            sendSafety(it.connection, message.toString())
        }

        message.put("own", false)
        activeUsers.forEach {
            if (it.key != character.userId) {
                it.value.connections.forEach { conn -> sendSafety(conn.connection, message.toString()) }
            }
        }
        logger.info("Session #$sessionId for user #${character.userId}: add active character #${character.id}")

        processingMovePropertiesInAdding(characterIdForMoveBeforeAdding, character.id)
    }

    private suspend fun processingMovePropertiesInAdding(characterIdForMoveBeforeAdding: UInt?, characterId: UInt) {
        val characterIdForMoveAfterAdding = getCurrentCharacterForMoveId()
        if (characterIdForMoveBeforeAdding != characterIdForMoveAfterAdding) {
            assert(characterIdForMoveAfterAdding == characterId)
            sendCharacterStatus(characterIdForMoveBeforeAdding, false)
            sendCharacterStatus(characterIdForMoveAfterAdding, true)
        } else if (characterIdForMoveBeforeAdding == characterId) {
            sendCharacterStatus(characterId, true)
        }
    }

    suspend fun removeCharacter(character: CharacterInfo) {
        val characterIdForMoveBeforeRemoving = getCurrentCharacterForMoveId()

        val userData = activeUsers.getValue(character.userId)
        userData.characters.remove(character.id)

        val message = JSONObject()
            .put("type", "character:leave")
            .put("id", character.id)
        activeUsers.forEach {
            it.value.connections.forEach { conn -> sendSafety(conn.connection, message.toString()) }
        }
        logger.info("Session #$sessionId for user #${character.userId}: remove active character #${character.id}")

        if (character.id == characterIdForMoveBeforeRemoving) {
            sendCharacterStatus(getCurrentCharacterForMoveId(), true)
        }
    }

    suspend fun moveCharacter(character: CharacterInfo) {
        val message = JSONObject(Json.encodeToString(character))
            .put("type", "character:move")
            .put("row", character.row)
            .put("col", character.col)
        activeUsers.forEach {
            it.value.connections.forEach { conn -> sendSafety(conn.connection, message.toString()) }
        }
        logger.info("Session #$sessionId for user #${character.userId}: " +
                "move character #${character.id} to (${character.row}, ${character.col})")

        sendCharacterStatus(moveProperties.prevCharacterMovedId.get().toUInt(), false)
        sendCharacterStatus(getCurrentCharacterForMoveId(), true)
    }

    suspend fun simpleAttack(characterId: UInt, opponentId: UInt) {
        val updatedCharacter = DBOperator.getCharacterByID(characterId)!!
        val updatedOpponent = DBOperator.getCharacterByID(opponentId)!!
        val message = JSONObject()
            .put("type", "character:attack")
            .put("attackType", "simple")
            .put("character", JSONObject(Json.encodeToString(updatedCharacter)))
            .put("opponent", JSONObject(Json.encodeToString(updatedOpponent)))

        activeUsers.forEach {
            it.value.connections.forEach { conn -> sendSafety(conn.connection, message.toString()) }
        }
        logger.info("Session #$sessionId for user #${updatedCharacter.userId}: " +
                "attack from character #$characterId to character #$opponentId")

        sendCharacterStatus(moveProperties.prevCharacterMovedId.get().toUInt(), false)
        sendCharacterStatus(getCurrentCharacterForMoveId(), true)
    }

    fun processingSimpleAttack(characterId: UInt, opponentId: UInt) {
        // TODO: fix properties in db, now don't work
        val characterDamage = DBOperator.getPropertyOfCharacter(characterId, "damage")!!
        val opponentHealth = DBOperator.getPropertyOfCharacter(opponentId, "health")!!
        DBOperator.setCharacterProperty(opponentId, "health", opponentHealth - characterDamage)
        logger.info("Session #$sessionId: change health of character #${opponentId} in db")
    }

    private fun getCurrentCharacterForMoveId(): UInt? {
        val charactersIdInOrderAdded = activeUsers
            .map { it.value.characters }
            .reduce { res, add -> res.plus(add).toMutableSet() }
            .toSortedSet()
        if (charactersIdInOrderAdded.isEmpty()) return null

        return charactersIdInOrderAdded.firstOrNull {
            it.toInt() > moveProperties.prevCharacterMovedId.get()
        } ?: charactersIdInOrderAdded.first()
    }

    fun validateMoveCharacter(mapId: UInt, row: Int, col: Int) {
        val map = DBOperator.getMapByID(mapId)?.load()
            ?: throw Exception("Map #$mapId does not exist")
        if (map.isObstacleTile(row, col)) throw Exception("Can't move: target tile is obstacle")
    }

    fun validateSimpleAttack(character: CharacterInfo, opponent: CharacterInfo) {
        if (abs(character.row - opponent.row) > 1 || abs(character.col - opponent.col) > 1)
            throw Exception("Can't move: too far to attack")
    }

    fun validateMoveAndUpdateMoveProperties(characterId: UInt) {
        val characterCanMoveId = getCurrentCharacterForMoveId()
        if (characterCanMoveId != characterId) throw Exception("Can't move: not your move now")

        moveProperties.prevCharacterMovedId = AtomicInteger(characterCanMoveId.toInt())
    }

    private suspend fun sendCharacterStatus(characterId: UInt?, canMove: Boolean) {
        if (characterId == null) return

        if (canMove) {
            logger.info("Session #$sessionId: now can move character #$characterId")
        }

        val messageStatus = JSONObject()
            .put("type", "character:status")
            .put("id", characterId)
            .put("can_move", canMove)
        val character = DBOperator.getCharacterByID(characterId)
            ?: throw Exception("Character with ID $characterId does not exist")
        activeUsers.getValue(character.userId).connections.forEach {
            sendSafety(it.connection, messageStatus.toString())
        }
    }

    private suspend fun sendCharacterStatusToConn(characterId: UInt, canMove: Boolean, connection: Connection) {
        if (canMove) {
            logger.info("Session #$sessionId: now can move character #$characterId")
        }

        val messageStatus = JSONObject()
            .put("type", "character:status")
            .put("id", characterId)
            .put("can_move", canMove)
        sendSafety(connection.connection, messageStatus.toString())
    }
}

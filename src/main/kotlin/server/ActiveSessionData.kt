package server

import db.CharacterInfo
import db.DBOperator
import db.Map.Companion.Position
import db.SessionInfo

import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import server.utils.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

const val initPrevCharacterId: Int = -1

class ActiveSessionData(
    val sessionId: UInt,
    val mapId: UInt,
    val started: Instant,
    var actionProperties: ActionProperties = ActionProperties(),
    val activeUsers: MutableMap<UInt, UserData> = Collections.synchronizedMap(mutableMapOf())
) {
    data class UserData(
        val userId: UInt,
        val sessionId: UInt,
        val connections: MutableSet<Connection> = Collections.synchronizedSet(mutableSetOf()),
        var characters: MutableSet<UInt> = Collections.synchronizedSet(mutableSetOf())
    )

    data class ActionProperties(
        var prevCharacterId: AtomicInteger = AtomicInteger(initPrevCharacterId)
    )

    constructor(sessionInfo: SessionInfo): this(
        sessionInfo.id,
        sessionInfo.mapID,
        sessionInfo.started,
        ActionProperties(AtomicInteger(sessionInfo.prevCharacterId))
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
            actionProperties.prevCharacterId.get()
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
                val curCharacterForActionId = getCurrentCharacterForActionId()
                if (it == curCharacterForActionId) {
                    sendCharacterActionStatusToConn(it, true, connection)
                }
            }
        }
    }

    // should not throw exceptions
    suspend fun finishConnection(userId: UInt, connection: Connection) {
        val userData = activeUsers.getValue(userId)
        userData.connections.remove(connection)
        if (userData.connections.isEmpty()) {
            DBOperator.getAllCharactersOfUserInSession(userId, sessionId).forEach {
                removeCharacter(it)
            }
            activeUsers.remove(userId)
        }
    }

    private suspend fun showCharacter(character: CharacterInfo, connection: Connection, own: Boolean) {
        val characterJson = JSONObject()
            .put("type", "character:new")
            .put("character", JSONObject(Json.encodeToString(character)))
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
        val characterForActionBeforeAddingId = getCurrentCharacterForActionId()

        val userData = activeUsers.getValue(character.userId)
        userData.characters.add(character.id)

        val message = JSONObject()
            .put("type", "character:new")
            .put("character", JSONObject(Json.encodeToString(character)))
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

        processingActionPropertiesInAdding(characterForActionBeforeAddingId, character.id)
    }

    private suspend fun processingActionPropertiesInAdding(characterForActionBeforeAddingId: UInt?, characterId: UInt) {
        val characterForActionAfterAddingId = getCurrentCharacterForActionId()
        if (characterForActionBeforeAddingId != characterForActionAfterAddingId) {
            assert(characterForActionAfterAddingId == characterId)
            sendCharacterActionStatus(characterForActionBeforeAddingId, false)
            sendCharacterActionStatus(characterForActionAfterAddingId, true)
        } else if (characterForActionBeforeAddingId == characterId) {
            sendCharacterActionStatus(characterId, true)
        }
    }

    suspend fun removeCharacter(character: CharacterInfo) {
        val characterForActionBeforeRemovingId = getCurrentCharacterForActionId()

        val userData = activeUsers.getValue(character.userId)
        userData.characters.remove(character.id)

        val message = JSONObject()
            .put("type", "character:leave")
            .put("id", character.id.toLong())
        activeUsers.forEach {
            it.value.connections.forEach { conn -> sendSafety(conn.connection, message.toString()) }
        }
        logger.info("Session #$sessionId for user #${character.userId}: remove active character #${character.id}")

        if (character.id == characterForActionBeforeRemovingId) {
            sendCharacterActionStatus(getCurrentCharacterForActionId(), true)
        }
    }

    suspend fun moveCharacter(character: CharacterInfo) {
        val message = JSONObject()
            .put("type", "character:move")
            .put("id", character.id.toLong())
            .put("character", JSONObject(Json.encodeToString(character)))
        activeUsers.forEach {
            it.value.connections.forEach { conn -> sendSafety(conn.connection, message.toString()) }
        }
        logger.info("Session #$sessionId for user #${character.userId}: " +
                "move character #${character.id} to (${character.row}, ${character.col})")

        updateActionProperties()
    }

    suspend fun attackOneWithoutCounterAttack(characterId: UInt, opponentId: UInt, type: String) {
        val updatedCharacter = DBOperator.getCharacterByID(characterId)!!
        val updatedOpponent = DBOperator.getCharacterByID(opponentId)!!
        val message = JSONObject()
            .put("type", "character:attack")
            .put("attackType", type)
            .put("character", JSONObject(Json.encodeToString(updatedCharacter)))
            .put("opponent", JSONObject(Json.encodeToString(updatedOpponent)))

        activeUsers.forEach {
            it.value.connections.forEach { conn -> sendSafety(conn.connection, message.toString()) }
        }
        logger.info("Session #$sessionId for user #${updatedCharacter.userId}: " + type +
            " attack from character #$characterId to character #$opponentId")

        updateActionProperties()
    }

    fun processingMeleeAttack(characterId: UInt, opponentId: UInt) {
        val damage = DBOperator.getCharacterProperty(characterId, "MELEE_AT_DMG")!!
        val oppHealth = DBOperator.getCharacterProperty(opponentId, "CURR_HP")!!

        DBOperator.setCharacterProperty(opponentId, "CURR_HP", oppHealth - damage)
        logger.info("Session #$sessionId: change \"CURR_HP\" of character #${opponentId} in db")
    }

    fun processingRangedAttack(characterId: UInt, opponentId: UInt) {
        val damage = DBOperator.getCharacterProperty(characterId, "RANGED_AT_DMG")!!
        val oppHealth = DBOperator.getCharacterProperty(opponentId, "CURR_HP")!!

        DBOperator.setCharacterProperty(opponentId, "CURR_HP", oppHealth - damage)
        logger.info("Session #$sessionId: change \"CURR_HP\" of character #${opponentId} in db")
    }

    fun processingMagicAttack(characterId: UInt, opponentId: UInt) {
        val damage = DBOperator.getCharacterProperty(characterId, "MAGIC_AT_DMG")!!
        val oppHealth = DBOperator.getCharacterProperty(opponentId, "CURR_HP")!!
        val currentMana = DBOperator.getCharacterProperty(characterId, "CURR_MP")!!
        val magicAttackCost = DBOperator.getCharacterProperty(characterId, "MAGIC_AT_COST")!!

        DBOperator.setCharacterProperty(opponentId, "CURR_HP", oppHealth - damage)
        logger.info("Session #$sessionId: change \"CURR_HP\" of character #${opponentId} in db")

        DBOperator.setCharacterProperty(characterId, "CURR_MP", currentMana - magicAttackCost)
        logger.info("Session #$sessionId: change \"CURR_MP\" of character #${characterId} in db")
    }

    private fun getCurrentCharacterForActionId(): UInt? {
        val charactersIdInOrderAdded = activeUsers
            .map { it.value.characters }
            .reduce { res, add -> res.plus(add).toMutableSet() }
            .toSortedSet()
        if (charactersIdInOrderAdded.isEmpty()) return null

        return charactersIdInOrderAdded.firstOrNull {
            it.toInt() > actionProperties.prevCharacterId.get()
        } ?: charactersIdInOrderAdded.first()
    }

    fun validateMoveCharacter(character: CharacterInfo, mapId: UInt, pos: Position) {
        val map = DBOperator.getMapByID(mapId)?.load()
            ?: throw Exception("Map #$mapId does not exist")
        if (map.isObstacleTile(pos))
            throw MoveException(MoveFailReason.TileObstacle, "Can't move: target tile is obstacle")

        val distance = DBOperator.getCharacterProperty(character.id, "SPEED")!!
        if (!map.checkDistance(Position(character.row, character.col), pos, distance))
            throw MoveException(MoveFailReason.BigDist, "Can't move: target tile is too far")
    }

    fun processTileEffects(characterId: UInt, mapId: UInt, pos: Position): CharacterInfo {
        val map = DBOperator.getMapByID(mapId)?.load()
            ?: throw Exception("Map #$mapId does not exist")
        val healthUpdate = map.getTileHealthUpdate(pos)
        val manaUpdate = map.getTileManaUpdate(pos)

        if (healthUpdate != 0) {
            val health = DBOperator.getCharacterProperty(characterId, "CURR_HP")!!
            val maxHealth = DBOperator.getCharacterProperty(characterId, "MAX_HP")!!
            DBOperator.setCharacterProperty(characterId, "CURR_HP", min(health + healthUpdate, maxHealth))
            logger.info("Session #$sessionId: change \"CURR_HP\" of character #${characterId} in db")
        }

        if (manaUpdate != 0) {
            val mana = DBOperator.getCharacterProperty(characterId, "CURR_MP")!!
            val maxMana = DBOperator.getCharacterProperty(characterId, "MAX_MP")!!
            DBOperator.setCharacterProperty(characterId, "CURR_MP", min(mana + healthUpdate, maxMana))
            logger.info("Session #$sessionId: change \"CURR_MP\" of character #${characterId} in db")
        }

        return DBOperator.getCharacterByID(characterId)!!
    }

    private fun inAttackRange(character: CharacterInfo, opponent: CharacterInfo, distance: Int): Boolean {
        return abs(character.row - opponent.row) <= distance && abs(character.col - opponent.col) <= distance
    }

    fun validateMeleeAttack(character: CharacterInfo, opponent: CharacterInfo) {
        if (!inAttackRange(character, opponent, 1))
            throw AttackException("melee", AttackFailReason.BigDist, "Can't attack: too far for melee attack")
    }

    fun validateRangedAttack(character: CharacterInfo, opponent: CharacterInfo) {
        val attackDistance = DBOperator.getCharacterProperty(character.id, "RANGED_AT_DIST")!!
        if (!inAttackRange(character, opponent, attackDistance))
            throw AttackException("ranged", AttackFailReason.BigDist, "Can't attack: too far for ranged attack")
    }

    fun validateMagicAttack(character: CharacterInfo, opponent: CharacterInfo) {
        val attackDistance = DBOperator.getCharacterProperty(character.id, "MAGIC_AT_DIST")!!
        if (!inAttackRange(character, opponent, attackDistance))
            throw AttackException("magic", AttackFailReason.BigDist, "Can't attack: too far for magic attack")

        val characterCurrentMana = DBOperator.getCharacterProperty(character.id, "CURR_MP")!!
        val characterMagicAttackCost = DBOperator.getCharacterProperty(character.id, "MAGIC_AT_COST")!!
        if (characterCurrentMana < characterMagicAttackCost) {
            throw AttackException("magic", AttackFailReason.LowMana, "Can't attack: too low mana for magic attack")
        }
    }

    fun validateAction(character: CharacterInfo) {
        val characterForActionId = getCurrentCharacterForActionId()
        if (characterForActionId != character.id)
            throw ActionException(ActionFailReason.NotYourTurn, "Can't do action: not your turn now")

        if (character.isDefeated) {
            throw ActionException(ActionFailReason.IsDefeated, "Can't do action: character is defeated")
        }
    }

    suspend fun updateActionProperties() {
        actionProperties.prevCharacterId = AtomicInteger(getCurrentCharacterForActionId()!!.toInt())

        sendCharacterActionStatus(actionProperties.prevCharacterId.get().toUInt(), false)
        sendCharacterActionStatus(getCurrentCharacterForActionId(), true)
    }

    private suspend fun sendCharacterActionStatus(characterId: UInt?, canDoAction: Boolean) {
        if (characterId == null) return

        if (canDoAction) {
            logger.info("Session #$sessionId: now can do action character #$characterId")
        }

        val messageStatus = JSONObject()
            .put("type", "character:status")
            .put("id", characterId.toLong())
            .put("can_do_action", canDoAction)
        val character = DBOperator.getCharacterByID(characterId)
            ?: throw Exception("Character with ID $characterId does not exist")
        activeUsers.getValue(character.userId).connections.forEach {
            sendSafety(it.connection, messageStatus.toString())
        }
    }

    private suspend fun sendCharacterActionStatusToConn(characterId: UInt, canDoAction: Boolean, connection: Connection) {
        if (canDoAction) {
            logger.info("Session #$sessionId: now can do action character #$characterId")
        }

        val messageStatus = JSONObject()
            .put("type", "character:status")
            .put("id", characterId.toLong())
            .put("can_do_action", canDoAction)
        sendSafety(connection.connection, messageStatus.toString())
    }

    fun validateRevival(character: CharacterInfo) {
        val characterForActionId = getCurrentCharacterForActionId()
        if (characterForActionId != character.id)
            throw ActionException(ActionFailReason.NotYourTurn, "Can't reborn: not your turn now")
    }

    fun processingRevival(characterId: UInt): CharacterInfo? {
        if (Random.nextInt(100) < 34) {
            DBOperator.setCharacterDefeatedStatus(characterId, false)
            DBOperator.setCharacterProperty(characterId, "CURR_HP", 1)
            return DBOperator.getCharacterByID(characterId)
        }
        return null
    }

    suspend fun sendCharacterDefeatedStatus(character: CharacterInfo, isDefeated: Boolean) {
        assert(character.isDefeated == isDefeated)

        val message = JSONObject()
            .put("type", "character:status")
            .put("is_defeated", isDefeated)
            .put("id", character.id.toLong())
            .put("character", JSONObject(Json.encodeToString(character)))

        activeUsers.forEach {
            it.value.connections.forEach { conn -> sendSafety(conn.connection, message.toString()) }
        }

        val info = if (isDefeated) "defeat" else "revive"
        logger.info("Session #$sessionId for user #${character.userId}: $info character #${character.id}")
    }

    suspend fun checkIfDefeated(characterId: UInt) {
        val health = DBOperator.getCharacterProperty(characterId, "CURR_HP")!!
        if (health <= 0) {
            DBOperator.setCharacterDefeatedStatus(characterId, true)
            sendCharacterDefeatedStatus(DBOperator.getCharacterByID(characterId)!!, true)
        }
    }
}

package server

import db.CharacterInfo
import db.DBOperator
import db.SessionInfo
import io.ktor.websocket.*
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

const val initPrevCharacterMovedId: Int = -1

class ActiveSessionData(
    val sessionId: UInt,
    val mapId: UInt,
    val started: Instant,
    var moveProperties: MoveProperties = MoveProperties(),
    val connections: MutableList<Connection> = Collections.synchronizedList(mutableListOf()),
    val charactersToConnection: MutableMap<UInt, Connection> = Collections.synchronizedMap(mutableMapOf())
) {
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

    private fun getCurrentCharacterForMoveId(): UInt? {
        if (charactersToConnection.isEmpty()) return null

        val charactersIdInOrderAdded = charactersToConnection.keys.toSortedSet()
        return charactersIdInOrderAdded.firstOrNull {
            it.toInt() > moveProperties.prevCharacterMovedId.get()
        } ?: charactersIdInOrderAdded.first()
    }

    fun validateMoveAndUpdateMoveProperties(characterId: UInt, mapId: UInt, row: Int, col: Int) {
        val map = DBOperator.getMapByID(mapId)?.load()
            ?: throw Exception("Map #$mapId does not exist")
        if (map.isObstacleTile(row, col)) throw Exception("Tile is obstacle")

        val characterCanMoveId = getCurrentCharacterForMoveId()
        if (characterCanMoveId != characterId) throw Exception("Can not move now")

        moveProperties.prevCharacterMovedId = AtomicInteger(characterCanMoveId.toInt())
    }

    private suspend fun sendCharacterStatus(id: UInt?, canMove: Boolean) {
        if (id == null) return

        val messageStatus = JSONObject()
            .put("type", "character:status")
            .put("id", id)
            .put("can_move", canMove)
        charactersToConnection.getValue(id).connection.send(messageStatus.toString())
    }

    private suspend fun updateCharactersStatus() {
        val prevId = moveProperties.prevCharacterMovedId.get().toUInt()
        sendCharacterStatus(prevId, false)

        val curId = getCurrentCharacterForMoveId()
        sendCharacterStatus(curId, true)
    }

    private suspend fun showCharacter(character: CharacterInfo, connection: Connection, own: Boolean) {
        val characterJson = JSONObject(Json.encodeToString(character))
            .put("type", "character:new")
            .put("own", own)
        connection.connection.send(characterJson.toString())
    }

    suspend fun startConnection(userId: UInt, connection: Connection, address: String) {
        connections.add(connection)
        logger.info("WebSocket: start connection with $address")

        charactersToConnection.forEach {
            showCharacter(
                DBOperator.getCharacterByID(it.key)
                    ?: throw Exception("Character with ID ${it.key} does not exist"),
                connection,
                false
            )
        }
        logger.info("WebSocket: characters in session $sessionId to user $userId")

        DBOperator.getAllCharactersOfUserInSession(userId, sessionId).forEach {
            addCharacterToSession(it, connection)
        }
    }

    suspend fun finishConnection(userId: UInt, connection: Connection, address: String) {
        connections.remove(connection)
        logger.info("WebSocket: finish connection with $address")

        DBOperator.getAllCharactersOfUserInSession(userId, sessionId).forEach {
            removeCharacterFromSession(it.id)
        }
    }

    fun getValidCharacter(message: JSONObject, userId: UInt): CharacterInfo {
        val characterId = message.getInt("id").toUInt()
        val character = DBOperator.getCharacterByID(characterId)
            ?: throw Exception("Character with ID $characterId does not exist")
        if (character.userId != userId || character.sessionId != sessionId)
            throw Exception("Character with ID $characterId doesn't belong to you")
        return character
    }

    suspend fun addCharacterToSession(character: CharacterInfo, connection: Connection) {
        val curCharacterForMoveId = getCurrentCharacterForMoveId()
        charactersToConnection[character.id] = connection

        val characterJson = JSONObject(Json.encodeToString(character))
            .put("type", "character:new")
            .put("own", true)
        connection.connection.send(characterJson.toString())
        characterJson.put("own", false)
        connections.forEach {
            if (it.id != connection.id) {
                it.connection.send(characterJson.toString())
            }
        }
        logger.info("WebSocket: new character with ID ${character.id}")

        val newCurCharacterForMoveId = getCurrentCharacterForMoveId()
        if (curCharacterForMoveId != newCurCharacterForMoveId) {
            sendCharacterStatus(curCharacterForMoveId, false)
            sendCharacterStatus(newCurCharacterForMoveId, true)
        }
    }

    suspend fun removeCharacterFromSession(characterId: UInt) {
        val curCharacterForMoveId = getCurrentCharacterForMoveId()
        charactersToConnection.remove(characterId)

        val message = JSONObject()
            .put("type", "character:leave")
            .put("id", characterId)
        connections.forEach {
            it.connection.send(message.toString())
        }
        logger.info("WebSocket: remove character with ID $characterId")

        if (charactersToConnection.isNotEmpty() && characterId == curCharacterForMoveId) {
            sendCharacterStatus(getCurrentCharacterForMoveId(), true)
        }
    }

    suspend fun moveCharacter(character: CharacterInfo) {
        val message = JSONObject()
            .put("type", "character:move")
            .put("id", character.id)
            .put("row", character.row)
            .put("col", character.col)

        connections.forEach { it.connection.send(message.toString()) }
        logger.info("WebSocket: move character with ID ${character.id}")

        updateCharactersStatus()
    }
}

package server.routing

import db.CharacterInfo
import db.DBOperator
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import server.ActiveSessionData
import server.Connection
import server.handleWebsocketIncorrectMessage
import server.logger
import java.util.concurrent.atomic.AtomicInteger

suspend fun startConnection(session: ActiveSessionData, userId: UInt, connection: Connection, address: String) {
    session.connections.add(connection)
    logger.info("WebSocket: start connection with $address")

    session.characters.forEach {
        addCharacterToConn(
            DBOperator.getCharacterByID(it)
                ?: throw IllegalArgumentException("Character #$it not found in the database"),
            connection.connection, false)
    }
    logger.info("WebSocket: active characters in session ${session.sessionId} to $userId")
    for (character in DBOperator.getAllCharactersOfUserInSession(userId, session.sessionId).toSet()) {
        addCharacterToSession(character, connection, session)
    }
}

suspend fun finishConnection(session: ActiveSessionData, userId: UInt, connection: Connection, address: String) {
    session.characters
        .filter { DBOperator.getUserByID(userId)?.id == userId }
        .forEach { removeCharacterFromSession(it, session) }
    session.connections.remove(connection)
    logger.info("WebSocket: start connection with $address")
}

fun getValidCharacter(message: JSONObject, userId: UInt, sessionId: UInt): CharacterInfo {
    val characterId = message.getInt("id").toUInt()
    val character = DBOperator.getCharacterByID(characterId)
        ?: throw Exception("Character with ID $characterId does not exist")
    if (character.userId != userId || character.sessionId != sessionId)
        throw Exception("Character with ID $characterId doesn't belong to you")
    return character
}

suspend fun addCharacterToConn(character: CharacterInfo, conn: WebSocketServerSession, own: Boolean) {
    val characterJson = JSONObject(Json.encodeToString(character))
    characterJson.put("type", "character:new")
    characterJson.put("own", own)
    conn.send(characterJson.toString())
}

suspend fun addCharacterToSession(
    character: CharacterInfo,
    connection: Connection,
    session: ActiveSessionData
) {
    session.characters.add(character.id)

    val characterJson = JSONObject(Json.encodeToString(character))
    characterJson.put("type", "character:new")
    characterJson.put("own", true)
    connection.connection.send(characterJson.toString())
    characterJson.put("own", false)
    session.connections.forEach {
        if (it.id != connection.id) {
            it.connection.send(characterJson.toString())
        }
    }
    logger.info("WebSocket: new character with ID ${character.id}")
}

suspend fun removeCharacterFromSession(characterId: UInt, session: ActiveSessionData) {
    session.characters.remove(characterId)

    val message = JSONObject()
    message.put("type", "character:leave")
    message.put("id", characterId)
    session.connections.forEach { it.connection.send(message.toString()) }
    logger.info("WebSocket: remove character with ID $characterId")
}

suspend fun moveCharacter(character: CharacterInfo, session: ActiveSessionData) {
    val message = JSONObject()
    message.put("type", "character:move")
    message.put("id", character.id)
    message.put("row", character.row)
    message.put("col", character.col)
    session.connections.forEach { it.connection.send(message.toString()) }
    logger.info("WebSocket: move character with ID ${character.id}")
}

fun Route.requestsConnection(activeSessions: MutableMap<UInt, ActiveSessionData>) {
    webSocket("/api/connect/{userId}/{sessionId}") {
        val userIdPrev = call.parameters["userId"]?.toUIntOrNull()
        val sessionIdPrev = call.parameters["sessionId"]?.toUIntOrNull()
        if (userIdPrev == null || sessionIdPrev == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid userId or sessionId: must be UInt"))
        }

        val userId = userIdPrev!!
        val sessionId = sessionIdPrev!!
        if (!activeSessions.contains(sessionId)) {
            val session = DBOperator.getSessionByID(sessionId)
            if (session != null) {
                activeSessions[sessionId] = ActiveSessionData(session)
            }
            else {
                // TODO: изменить поведение при несуществующей сессии
                throw IllegalArgumentException("Active session #sessionId does not exist")
                // val newSession = ActiveSessionData(DBOperator.addSession())
                // activeSessions[sessionId] = newSession
            }
        }
        val session = activeSessions.getValue(sessionId)

        val connection = Connection(this)
        val connectionId = connection.id

        try {
            startConnection(session, userId, connection, call.request.origin.remoteAddress)

            if (session.connections.size == 1) {
                session.moveProperties.whoCanMove = AtomicInteger(connectionId)
            }

            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val frameText = frame.readText()
                val message = JSONObject(frameText)
                if (!message.has("type")) throw Exception("Invalid websocket message: missing field \"type\"")
                when (message.getString("type")) {
                    "character:new" -> {
                        try {
                            val characterName = message.optString("name", "Dovakin")
                            val characterRow = message.optInt("row", 1)
                            val characterCol = message.optInt("col", 1)

                            val character = DBOperator.addCharacter(
                                userId,
                                sessionId,
                                characterName,
                                characterRow,
                                characterCol)
                            addCharacterToSession(character, connection, session)
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:new", e)
                        }
                    }
                    "character:remove" -> {
                        try {
                            val character = getValidCharacter(message, userId, sessionId)
                            DBOperator.deleteCharacterById(character.id)
                            removeCharacterFromSession(character.id, session)
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:remove", e)
                        }
                    }
                    "character:move" -> {
                        try {
                            val character = getValidCharacter(message, userId, sessionId)
                            val newRow = message.getInt("row")
                            val newCol = message.getInt("col")

                            session.validateMove(connectionId)

                            val newCharacter = DBOperator.moveCharacter(character.id, newRow, newCol)
                            moveCharacter(newCharacter!!, session)

                            session.updateMoveProperties()
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:move", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            handleWebsocketIncorrectMessage(this, userId, "", e)
        } finally {
            finishConnection(session, userId, connection, call.request.origin.remoteAddress)

            if (session.connections.isEmpty()) {
                // TODO: add DBOperator.updateSession
                DBOperator.updateSession(session.toSessionInfo())
                DBOperator.setSessionActive(sessionId, false)
                activeSessions.remove(sessionId)
            }
        }
    }
}

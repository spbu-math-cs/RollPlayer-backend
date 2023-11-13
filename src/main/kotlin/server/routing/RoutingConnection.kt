package server.routing

import db.DBOperator
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.json.JSONObject
import server.ActiveSessionData
import server.Connection
import server.handleWebsocketIncorrectMessage

fun Route.connection(activeSessions: MutableMap<UInt, ActiveSessionData>) {
    webSocket("/api/connect/{userId}/{sessionId}") {
        val userIdPrev = call.parameters["userId"]?.toUIntOrNull()
        val sessionIdPrev = call.parameters["sessionId"]?.toUIntOrNull()
        if (userIdPrev == null || sessionIdPrev == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid userId or sessionId: must be UInt"))
        }

        val userId = userIdPrev!!
        if (DBOperator.getUserByID(userId) == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid userId: user does not exist"))
        }

        val sessionId = sessionIdPrev!!
        val sessionFromDB = DBOperator.getSessionByID(sessionId)
        if (sessionFromDB == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid sessionId: session does not exist"))
        }
        if (!activeSessions.contains(sessionId)) {
            activeSessions[sessionId] = ActiveSessionData(sessionFromDB!!)
            DBOperator.setSessionActive(sessionId, true)
        }
        val session = activeSessions.getValue(sessionId)

        val conn = Connection(this)

        try {
            conn.connection.send(session.toJson())
            session.startConnection(userId, conn, call.request.origin.remoteAddress)

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
                            session.addCharacterToSession(character, conn)
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:new", e)
                        }
                    }
                    "character:remove" -> {
                        try {
                            val character = session.getValidCharacter(message, userId)

                            DBOperator.deleteCharacterById(character.id)
                            session.removeCharacterFromSession(character.id)
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:remove", e)
                        }
                    }
                    "character:move" -> {
                        try {
                            val character = session.getValidCharacter(message, userId)
                            val newRow = message.getInt("row")
                            val newCol = message.getInt("col")

                            session.validateMoveAndUpdateMoveProperties(character.id)

                            val newCharacter = DBOperator.moveCharacter(character.id, newRow, newCol)
                            session.moveCharacter(newCharacter!!)
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:move", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            handleWebsocketIncorrectMessage(this, userId, "", e)
        } finally {
            session.finishConnection(userId, conn, call.request.origin.remoteAddress)

            if (session.connections.isEmpty()) {
                DBOperator.updateSession(session.toSessionInfo())
                DBOperator.setSessionActive(sessionId, false)
                activeSessions.remove(sessionId)
            }
        }
    }
}

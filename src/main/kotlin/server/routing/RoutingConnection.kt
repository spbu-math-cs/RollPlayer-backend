package server.routing

import db.DBOperator

import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.json.JSONArray
import org.json.JSONObject
import server.ActiveSessionData
import server.Connection
import server.handleWebsocketIncorrectMessage
import server.logger

fun characterPropsToMap(characterProps: JSONArray?): Map<String, Int> {
    val characterPropsMap = mutableMapOf<String, Int>()
    if (characterProps != null) {
        for (i in 0 until characterProps.length()) {
            val prop = characterProps.getJSONObject(i)
            characterPropsMap[prop.getString("name")] = prop.getInt("value")
        }
    }
    return characterPropsMap
}

fun Route.connection(activeSessions: MutableMap<UInt, ActiveSessionData>) {
    webSocket("/api/connect/{userId}/{sessionId}") {
        val userId = call.parameters["userId"]?.toUIntOrNull()
        val sessionId = call.parameters["sessionId"]?.toUIntOrNull()
        if (userId == null || sessionId == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid userId or sessionId: must be UInt"))
            return@webSocket
        }

        if (DBOperator.getUserByID(userId) == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid userId: user does not exist"))
            return@webSocket
        }

        val sessionFromDB = DBOperator.getSessionByID(sessionId)
        if (sessionFromDB == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid sessionId: session does not exist"))
            return@webSocket
        }
        if (!activeSessions.contains(sessionId)) {
            activeSessions[sessionId] = ActiveSessionData(sessionFromDB!!)
            DBOperator.setSessionActive(sessionId, true)
        }
        val session = activeSessions.getValue(sessionId)

        val conn = Connection(this, userId)

        try {
            session.startConnection(userId, conn)
            logger.info("WebSocket: start connection with ${call.request.origin.remoteAddress}")

            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val frameText = frame.readText()
                val message = JSONObject(frameText)
                if (!message.has("type")) throw Exception("Invalid websocket message: missing field \"type\"")
                when (message.getString("type")) {
                    "character:new" -> {
                        try {
                            val characterName = message.optString("name", "Dovakin")
                            val characterRow = message.optInt("row", 0)
                            val characterCol = message.optInt("col", 0)
                            val characterProps = characterPropsToMap(message.optJSONArray("properties"))

                            val character = DBOperator.addCharacter(
                                userId,
                                sessionId,
                                characterName,
                                characterRow,
                                characterCol,
                                characterProps
                            )
                            session.addCharacter(character)
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:new", e)
                        }
                    }
                    "character:remove" -> {
                        try {
                            val character = session.getValidCharacter(message, userId)

                            DBOperator.deleteCharacterById(character.id)
                            session.removeCharacter(character)
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:remove", e)
                        }
                    }
                    "character:move" -> {
                        try {
                            val character = session.getValidCharacter(message, userId)
                            val newRow = message.getInt("row")
                            val newCol = message.getInt("col")

                            session.validateMoveCharacter(session.mapId, newRow, newCol)
                            session.validateMoveAndUpdateMoveProperties(character.id)

                            val newCharacter = DBOperator.moveCharacter(character.id, newRow, newCol)
                            session.moveCharacter(newCharacter!!)
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:move", e)
                        }
                    }
                    "character:attack" -> {
                        try {
                            when (message.optString("type-attack", "simple")) {
                                "simple" -> {
                                    val character = session.getValidCharacter(message, userId)
                                    val opponent = session.getValidOpponentCharacter(message)

                                    session.validateSimpleAttack(character, opponent)
                                    session.validateMoveAndUpdateMoveProperties(character.id)

                                    session.simpleAttack(character.id, opponent.id)
                                }
                                else -> {
                                    throw Exception("Incorrect field \"type-attack\" in message")
                                }
                            }
                        } catch (e: Exception) {
                            handleWebsocketIncorrectMessage(this, userId, "character:attack", e)
                        }
                    }
                    else -> {
                        throw Exception("Incorrect field \"type\" in message")
                    }
                }
            }
        } catch (e: Exception) {
            handleWebsocketIncorrectMessage(this, userId, "", e)
        } finally {
            session.finishConnection(userId, conn)
            logger.info("WebSocket: finish connection with ${call.request.origin.remoteAddress}")

            if (session.activeUsers.isEmpty()) {
                DBOperator.updateSession(session.toSessionInfo())
                DBOperator.setSessionActive(sessionId, false)
                activeSessions.remove(sessionId)
            }
        }
    }
}

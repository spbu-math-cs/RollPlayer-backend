package server.routing

import db.BasicProperties
import db.DBOperator
import db.Map.Companion.Position
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import org.json.JSONObject
import server.*
import server.utils.*

const val DEFAULT_CHARACTER_NAME = "Dovahkiin"

fun Route.gameSessionConnection(activeSessions: MutableMap<UInt, ActiveSessionData>) {
    authenticate("auth-jwt") {
        webSocket("/api/user/sessions/{sessionId}/connect") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asInt().toUInt()

            val sessionId = call.parameters["sessionId"]?.toUIntOrNull()
            if (sessionId == null) {
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
                activeSessions[sessionId] = ActiveSessionData(sessionFromDB)
                DBOperator.setSessionActive(sessionId, true)
            }
            val session = activeSessions.getValue(sessionId)

            val conn = Connection(this, userId, sessionId)

            try {
                logger.info("Session #$sessionId for user #$userId: start connection")
                session.startConnection(userId, conn)

                for (frame in incoming) {
                    try {
                        frame as? Frame.Text ?: continue
                        val frameText = frame.readText()
                        val message = JSONObject(frameText)
                        if (!message.has("type")) throw Exception("Invalid websocket message: missing field \"type\"")
                        when (message.getString("type")) {
                            "character:new" -> {
                                try {
                                    val characterName = message.optString("name", DEFAULT_CHARACTER_NAME)
                                    val characterRow = message.optInt("row")
                                    val characterCol = message.optInt("col")
                                    val characterBasicProps = Json.decodeFromString<BasicProperties>(
                                        message.optString("basicProperties", "{}")
                                    )
                                    val characterAvatarId = if (message.has("avatarId")) {
                                        message.getInt("avatarId").toUInt()
                                    } else {
                                        null
                                    }

                                    session.validateBasicProperties(characterBasicProps)
                                    session.validateTile(Position(characterRow, characterCol))

                                    val character = DBOperator.addCharacter(
                                        userId,
                                        sessionId,
                                        characterName,
                                        characterAvatarId,
                                        characterRow,
                                        characterCol,
                                        false,
                                        characterBasicProps
                                    )
                                    logger.info(
                                        "Session #$sessionId for user #$userId: " +
                                                "add new character #${character.id} in db"
                                    )

                                    session.addCharacter(character)
                                } catch (e: Exception) {
                                    handleWebsocketIncorrectMessage(conn, "character:new", e)
                                }
                            }

                            "character:remove" -> {
                                try {
                                    val character = session.getValidCharacter(message, userId)

                                    DBOperator.deleteCharacterById(character.id)
                                    logger.info(
                                        "Session #$sessionId for user #$userId: " +
                                                "delete character #${character.id} from db"
                                    )

                                    session.removeCharacter(character)
                                } catch (e: Exception) {
                                    handleWebsocketIncorrectMessage(conn, "character:remove", e)
                                }
                            }

                            "character:move" -> {
                                try {
                                    val character = session.getValidCharacter(message, userId)
                                    val newRow = message.getInt("row")
                                    val newCol = message.getInt("col")

                                    session.validateAction(character)
                                    session.validateMoveCharacter(character, Position(newRow, newCol))

                                    val newCharacter = DBOperator.moveCharacter(character.id, newRow, newCol)
                                    logger.info(
                                        "Session #$sessionId for user #$userId: " +
                                                "change coords of character #${character.id} in db"
                                    )

                                    session.processTileEffects(newCharacter!!.id, Position(newRow, newCol))

                                    session.moveCharacter(newCharacter)
                                    session.checkIfDefeated(character.id)
                                } catch (e: ActionException) {
                                    sendActionExceptionReason(conn, "character:move", e)
                                } catch (e: MoveException) {
                                    sendMoveExceptionReason(conn, e)
                                } catch (e: Exception) {
                                    handleWebsocketIncorrectMessage(conn, "character:move", e)
                                }
                            }

                            "character:attack" -> {
                                try {
                                    val character = session.getValidCharacter(message, userId)
                                    val opponent = session.getValidOpponentCharacter(message)
                                    val attackType = message.optString("attackType", "melee")

                                    session.validateAction(character)
                                    session.validateAttack(opponent)

                                    when (attackType) {
                                        "melee" -> {
                                            session.validateMeleeAttack(character, opponent)
                                            session.processingMeleeAttack(character.id, opponent.id)
                                        }

                                        "ranged" -> {
                                            session.validateRangedAttack(character, opponent)
                                            session.processingRangedAttack(character.id, opponent.id)
                                        }

                                        "magic" -> {
                                            session.validateMagicAttack(character, opponent)
                                            session.processingMagicAttack(character.id, opponent.id)
                                        }

                                        else -> {
                                            throw Exception("Incorrect field \"attackType\" in message")
                                        }
                                    }

                                    session.attackOneWithoutCounterAttack(character.id, opponent.id, attackType)
                                    session.checkIfDefeated(character.id)
                                    session.checkIfDefeated(opponent.id)
                                } catch (e: ActionException) {
                                    sendActionExceptionReason(conn, "character:attack", e)
                                } catch (e: AttackException) {
                                    sendAttackExceptionReason(conn, e)
                                } catch (e: Exception) {
                                    handleWebsocketIncorrectMessage(conn, "character:attack", e)
                                }
                            }

                            "character:revive" -> {
                                try {
                                    val character = session.getValidCharacter(message, userId)

                                    session.validateRevival(character)

                                    val characterAfterRevival = session.processingRevival(character.id)
                                    if (characterAfterRevival != null) {
                                        session.sendCharacterDefeatedStatus(characterAfterRevival, false)
                                    }
                                    session.updateActionProperties()
                                } catch (e: ReviveException) {
                                    sendReviveExceptionReason(conn, e)
                                } catch (e: Exception) {
                                    handleWebsocketIncorrectMessage(conn, "character:revive", e)
                                }
                            }

                            else -> {
                                throw Exception("Incorrect field \"type\" in message")
                            }
                        }
                    } catch (e: Exception) {
                        handleWebsocketIncorrectMessage(conn, "message parsing", e)
                    }
                }
            } catch (e: Exception) {
                handleWebsocketIncorrectMessage(conn, "startConnection", e)
            } finally {
                session.finishConnection(userId, conn)

                if (session.activeUsers.isEmpty()) {
                    DBOperator.updateSession(session.toSessionInfo())
                    DBOperator.setSessionActive(sessionId, false)
                    activeSessions.remove(sessionId)
                }
                logger.info("Session #$sessionId for user #$userId: finish connection")
            }
        }
    }
}

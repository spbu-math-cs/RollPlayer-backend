package server.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import org.json.JSONObject
import server.logger

suspend fun handleHTTPRequestException(
    call: ApplicationCall,
    requestInfo: String,
    e: Exception,
    additional: String = ""
) {
    logger.error("Failed $requestInfo request from ${call.request.origin.remoteAddress}", e)

    val errorMessage = if (additional.isEmpty()) e.message.orEmpty() else "$additional: ${e.message.orEmpty()}"
    call.respond(
        HttpStatusCode.BadRequest,
        mapOf("type" to "error", "message" to errorMessage)
    )
}

suspend fun handleWebsocketIncorrectMessage(
    connection: WebSocketServerSession,
    userId: UInt,
    on: String,
    e: Exception
) {
    logger.info("Failed websocket message type $on from user with ID $userId", e)
    sendSafety(connection, JSONObject(mapOf(
        "type" to "error",
        "on" to on,
        "message" to e.message.orEmpty()
    )).toString())
}

suspend fun sendAttackExceptionReason(
    connection: WebSocketServerSession,
    userId: UInt,
    e: AttackException
) {
    logger.info("Failed websocket message type character:attack from user with ID $userId", e)
    sendSafety(connection, JSONObject(mapOf(
        "type" to "error",
        "on" to "character:attack",
        "attackType" to e.attackType,
        "reason" to e.reason.str,
        "message" to e.message.orEmpty()
    )).toString())
}


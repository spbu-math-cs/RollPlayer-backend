package server.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import org.json.JSONObject
import server.Connection
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
    connection: Connection,
    on: String,
    e: Exception
) {
    logger.info("Session #${connection.sessionId} for user #${connection.userId}: failed $on", e)
    sendSafety(connection.connection, JSONObject(mapOf(
        "type" to "error",
        "on" to on,
        "message" to e.message.orEmpty()
    )).toString())
}

suspend fun sendActionExceptionReason(
    connection: Connection,
    on: String,
    e: ActionException
) {
    logger.info("Session #${connection.sessionId} for user #${connection.userId}: " +
            "failed $on because of ${e.reason.str}")
    sendSafety(connection.connection, JSONObject(mapOf(
        "type" to "error",
        "on" to on,
        "reason" to e.reason.str,
        "message" to e.message.orEmpty()
    )).toString())
}

suspend fun sendMoveExceptionReason(
    connection: Connection,
    e: MoveException
) {
    logger.info("Session #${connection.sessionId} for user #${connection.userId}: " +
            "failed character:move because of ${e.reason.str}")
    sendSafety(connection.connection, JSONObject(mapOf(
        "type" to "error",
        "on" to "character:move",
        "reason" to e.reason.str,
        "message" to e.message.orEmpty()
    )).toString())
}

suspend fun sendAttackExceptionReason(
    connection: Connection,
    e: AttackException
) {
    logger.info("Session #${connection.sessionId} for user #${connection.userId}: " +
            "failed character:attack because of ${e.reason.str}")
    sendSafety(connection.connection, JSONObject(mapOf(
        "type" to "error",
        "on" to "character:attack",
        "attackType" to e.attackType,
        "reason" to e.reason.str,
        "message" to e.message.orEmpty()
    )).toString())
}

suspend fun sendReviveExceptionReason(
    connection: Connection,
    e: ReviveException
) {
    logger.info("Session #${connection.sessionId} for user #${connection.userId}: " +
            "failed character:revive because of ${e.reason.str}")
    sendSafety(connection.connection, JSONObject(mapOf(
        "type" to "error",
        "on" to "character:revive",
        "reason" to e.reason.str,
        "message" to e.message.orEmpty()
    )).toString())
}

package server.routing

import db.DBOperator
import server.*

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import server.utils.handleHTTPRequestException

fun Route.gameSession() {
    post("/api/game/create") {
        try {
            val mapId = call.request.queryParameters["mapId"]?.toUIntOrNull()
                ?: throw Exception("Request must contain \"mapId\" query parameter")

            val session = DBOperator.addSession(mapId)
            call.respond(HttpStatusCode.OK, JSONObject()
                .put("type", "ok")
                .put("message", "Session created")
                .put("result", JSONObject(Json.encodeToString(session)))
                .toString()
            )

            logger.info("Successful POST /api/game/create request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "POST /api/game/create", e)
        }
    }

    get("/api/game/{sessionId}/mapId") {
        val sessionId = call.parameters["sessionId"]?.toUIntOrNull() ?: 0u
        try {
            val mapId = DBOperator.getSessionByID(sessionId)?.mapID
                ?: throw IllegalArgumentException("Session #$sessionId does not exist")
            call.respond(HttpStatusCode.OK, JSONObject()
                .put("type", "ok")
                .put("result", mapId)
                .toString()
            )

            logger.info("Successful GET /api/game/$sessionId/mapId request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "GET /api/game/$sessionId/mapId", e)
        }
    }
}

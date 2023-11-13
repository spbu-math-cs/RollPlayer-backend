package server.routing

import db.DBOperator
import io.ktor.http.*

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import server.handleHTTPRequestException

fun Route.createSession() {
    post("/api/game/create") {
        try {
            val mapId = call.request.queryParameters["mapId"]?.toUIntOrNull()
                ?: throw Exception("Request must contain \"mapId\" query parameter")

            DBOperator.addSession(mapId)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Session created"))
        } catch (e: Exception) {
            handleHTTPRequestException(call, "POST /api/game/create", e)
        }
    }
}

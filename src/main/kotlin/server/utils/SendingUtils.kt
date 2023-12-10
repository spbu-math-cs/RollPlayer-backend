package server.utils

import io.ktor.server.websocket.*
import io.ktor.websocket.*

suspend fun sendSafety(
    connection: WebSocketServerSession,
    content: String
) {
    try {
        connection.send(content)
    } catch (_: Exception) {}
}

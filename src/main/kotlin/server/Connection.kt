package server

import io.ktor.server.websocket.*
import java.util.concurrent.atomic.*

class Connection(val session: WebSocketServerSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }
    val id = lastId.getAndIncrement()
}

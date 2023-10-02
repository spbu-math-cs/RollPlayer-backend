import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.plugins.contentnegotiation.*

import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*

import java.time.Duration

data class PlayerInfo(var name: String, var x: Long, var y: Long)

fun Application.module() {
    val port = environment.config.propertyOrNull("ktor.deployment.port")
        ?.getString()?.toIntOrNull() ?: 9999
    val host = environment.config.propertyOrNull("ktor.deployment.host")
        ?.getString() ?: "127.0.0.1"

    embeddedServer(Netty, port = port, host = host) {
        extracted()
    }.start(wait = true)
}

private fun Application.extracted() {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(5)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    var playersWithID = mapOf<Long, PlayerInfo>()

    println("Server is ready")
    routing {
        webSocket("/echo") {
            send(Frame.Text("Привет, клиент!"))
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val receivedText = frame.readText()
                    println("Получено от клиента: $receivedText")
                    send(Frame.Text("Получено ваше сообщение: $receivedText"))
                }
            }
        }

//        //ws://localhost:8083/number
//        webSocket("/number") {
//            outgoing.send(Frame.Text("42"))
//        }
//
//        //ws://localhost:8083/sum/5/10
//        webSocket("/sum/{num1}/{num2}") {
//            val num1 = call.parameters["num1"]?.toIntOrNull()
//            val num2 = call.parameters["num2"]?.toIntOrNull()
//
//            if (num1 != null && num2 != null) {
//                outgoing.send(Frame.Text("Сумма чисел: ${num1 + num2}"))
//            } else {
//                outgoing.send(Frame.Text("Invalid numbers"))
//            }
//        }
//
//        // Получить все текстуры
//        get("/textures") {
//            call.respond(textures)
//        }
//
//        // Создать новую карту
//        post("/maps") {
//            val newMapId = maps.size + 1
//            val newMap = MapInfo(newMapId, call.parameters["name"] ?: "New Map", call.parameters["size"]?.toIntOrNull() ?: 100, List(100) { Cell("inactive") })
//            maps.add(newMap)
//            call.respond(newMap)
//        }
//
//        // Получить информацию о карте по ее ID
//        get("/maps/{id}") {
//            val mapId = call.parameters["id"]?.toIntOrNull()
//            val map = maps.find { it.id == mapId }
//            if (map != null) {
//                call.respond(map)
//            } else {
//                call.respondText("Map not found", status = HttpStatusCode.NotFound)
//            }
//        }
//
//        // Обновить информацию о карте по ее ID
//        put("/maps/{id}") {
//            val mapId = call.parameters["id"]?.toIntOrNull()
//            val existingMap = maps.find { it.id == mapId }
//            if (existingMap != null) {
//                val updateRequest = call.receive<UpdateMapRequest>()
//                existingMap.name = updateRequest.name
//                existingMap.size = updateRequest.size
//                existingMap.cells = updateRequest.cells
//                call.respond(existingMap)
//            } else {
//                call.respondText("Map not found", status = HttpStatusCode.NotFound)
//            }
//        }
    }
}

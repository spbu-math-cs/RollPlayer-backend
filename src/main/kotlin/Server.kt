import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration

//@Serializable
//data class AdditionRequest(val num1: Int, val num2: Int)
object Server {
    fun launch(config: ApplicationConfig) {
        val port = config.propertyOrNull("ktor.deployment.port")?.getString()?.toIntOrNull() ?: 8083
        val host = config.propertyOrNull("ktor.deployment.host")?.getString() ?: "0.0.0.0"

        embeddedServer(Netty, port = port, host = host) {

            println("Server is ready")

            install(ContentNegotiation) {
                json()
                //protobuf {}
            }


            install(WebSockets) {
                pingPeriod = Duration.ofMinutes(1)
            }

            routing {

                get("/") {
                    call.respond(HttpStatusCode.OK, "Hello, World!")
                }

//                webSocket("/sum/{num1}/{num2}") {
//                    try {
//                        val requestFrame = incoming.receive() as? Frame.Binary ?: return@webSocket
//                        val requestBytes = ByteArray(requestFrame.data.remaining())
//                        requestFrame.data.get(requestBytes)
//                        val additionRequest = AdditionRequest.parseFrom(requestBytes)
//
//                        val sum = additionRequest.num1 + additionRequest.num2
//                        val response = AdditionResponse.newBuilder().setSum(sum).build()
//
//                        val responseBytes = response.toByteArray()
//                        val responseFrame = Frame.Binary(true, ByteBuffer.wrap(responseBytes))
//                        outgoing.send(responseFrame)
//                    } catch (e: Exception) {
//                        println("Error: ${e.message}")
//                    }
//                }


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
                //ws://localhost:8083/number
                webSocket("/number") {
                    outgoing.send(Frame.Text("42"))
                }

                //ws://localhost:8083/sum/5/10
                webSocket("/sum/{num1}/{num2}") {
                    val num1 = call.parameters["num1"]?.toIntOrNull()
                    val num2 = call.parameters["num2"]?.toIntOrNull()

                    if (num1 != null && num2 != null) {
                        outgoing.send(Frame.Text("Сумма чисел: ${num1 + num2}"))
                    } else {
                        outgoing.send(Frame.Text("Invalid numbers"))
                    }
                }


                get("/number") {
                    val number = 42
                    call.respondText(number.toString(), ContentType.Text.Plain)
                }

                get("/sum/{num1}/{num2}") {
                    val num1 = call.parameters["num1"]?.toIntOrNull()
                    val num2 = call.parameters["num2"]?.toIntOrNull()

                    if (num1 != null && num2 != null) {
                        call.respondText("Сумма чисел: ${num1 + num2}", ContentType.Text.Plain)
                    } else {
                        call.respondText("Invalid numbers", ContentType.Text.Plain, HttpStatusCode.BadRequest)
                    }
                }

//                // Получить все текстуры
//                get("/textures") {
//                    call.respond(textures)
//                }

//                // Создать новую карту
//                post("/maps") {
//                    val newMapId = maps.size + 1
//                    val newMap = MapInfo(newMapId, call.parameters["name"] ?: "New Map", call.parameters["size"]?.toIntOrNull() ?: 100, List(100) { Cell("inactive") })
//                    maps.add(newMap)
//                    call.respond(newMap)
//                }

//                // Получить информацию о карте по ее ID
//                get("/maps/{id}") {
//                    val mapId = call.parameters["id"]?.toIntOrNull()
//                    val map = maps.find { it.id == mapId }
//                    if (map != null) {
//                        call.respond(map)
//                    } else {
//                        call.respondText("Map not found", status = HttpStatusCode.NotFound)
//                    }
//                }

//                // Обновить информацию о карте по ее ID
//                put("/maps/{id}") {
//                    val mapId = call.parameters["id"]?.toIntOrNull()
//                    val existingMap = maps.find { it.id == mapId }
//                    if (existingMap != null) {
//                        val updateRequest = call.receive<UpdateMapRequest>()
//                        existingMap.name = updateRequest.name
//                        existingMap.size = updateRequest.size
//                        existingMap.cells = updateRequest.cells
//                        call.respond(existingMap)
//                    } else {
//                        call.respondText("Map not found", status = HttpStatusCode.NotFound)
//                    }
//                }

            }
        }.start(wait = true)
    }
}


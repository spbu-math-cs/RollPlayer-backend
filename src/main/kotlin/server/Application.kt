package server

import db.DBOperator
import io.ktor.http.*
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.util.*

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

    DBOperator.connectOrCreate()
    // DBOperator.removeNonExistingMaps()

    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
    val playerPropertiesByID = mutableMapOf<Int, PlayerProperties>()
    val logger = LoggerFactory.getLogger(Application::class.java)

    suspend fun handleRequestError(call: ApplicationCall, path: String, e: Exception) {
        val errorInfo = e.localizedMessage ?: "Unknown Error"
        val errorMessage = "Error processing $path - $errorInfo"
        val logger = LoggerFactory.getLogger(Application::class.java)

        logger.error(errorMessage, e)
        call.respond(
            HttpStatusCode.BadRequest,
            mapOf("type" to "error", "message" to errorInfo)
        )
    }

    logger.info("Server is ready")
    routing {
        static {
            staticRootFolder = File("") // project root dir
            files("static") // dir for all static files
        }

        /** Send all textures */
        get("/api/textures") {
            try {
                val textures = DBOperator.getAllTextures()
                call.response.status(HttpStatusCode.OK)
                call.respond(textures.map { mapOf("id" to it.id.toString(), "url" to it.pathToFile) })
                logger.info("GET request: /api/textures was successful. Received from: ${call.request.origin.remoteHost}")
            } catch (e: Exception) {
                handleRequestError(call, "/api/textures", e)
            }
        }

        /** Send texture by ID */
        get("/api/textures/{id}") {
            val textureID = call.parameters["id"]?.toIntOrNull() ?: 0
            try {
                val textureFile = File(DBOperator.getTextureByID(textureID)?.pathToFile
                    ?: throw IllegalArgumentException("Texture #$textureID does not exist"))
                call.response.status(HttpStatusCode.OK)
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment
                        .withParameter(ContentDisposition.Parameters.FileName, textureFile.name)
                        .toString()
                )
                call.respondFile(textureFile)
                logger.info("Successful GET request to /api/textures/$textureID from ${call.request.origin.remoteHost}")
            } catch (e: Exception) {
                handleRequestError(call, "/api/textures/$textureID", e)
            }
        }

        /** Get json with some fields of PlayerProperties and new values */
        webSocket("/api/connect") {
            logger.info("WebSocket connection established with ${call.request.origin.remoteHost}")
            val thisConnection = Connection(this)
            connections += thisConnection
            val id = thisConnection.id

            send(createSimpleMap())
            playerPropertiesByID.forEach {
                send(Json.encodeToString(Player(it.key, it.value)))
            }
            playerPropertiesByID[id] = PlayerProperties(id)

            try {
                connections.forEach {
                    it.session.send(Json.encodeToString(Player(id, playerPropertiesByID.getValue(id))))
                }
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val newProperties = JSONObject(frame.readText())
                    val oldProperties = playerPropertiesByID.getValue(id)
                    playerPropertiesByID[id] = updateProperties(oldProperties, newProperties)
                    connections.forEach { otherConnection ->
                        if (otherConnection.id != id) {
                            logger.info("Sending player data from Player #$id to Player #${otherConnection.id}: ${Json.encodeToString(Player(id, playerPropertiesByID.getValue(id)))}")
                            otherConnection.session.send(Json.encodeToString(Player(id, playerPropertiesByID.getValue(id))))
                        }
                    }
                }
                val numberOfClients = connections.size
                logger.info("WebSocket messages sent to $numberOfClients clients: All clients, ${playerPropertiesByID.size} players.")
            } catch (e: Exception) {
                logger.error("WebSocket error: ${e.localizedMessage}")
            } finally {
                val oldProperties = playerPropertiesByID.getValue(id)
                oldProperties.status = PlayerStatus.DISCONNECTED
                playerPropertiesByID.remove(id)

                connections.forEach {
                    it.session.send(Json.encodeToString(Player(id, oldProperties)))
                }
                connections -= thisConnection
                logger.info("WebSocket connection closed with ${call.request.origin.remoteHost}.")
            }
        }
    }
}

package server

import db.DBOperator

import io.ktor.http.*
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.*
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
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.util.*

import org.json.JSONObject
import mu.KotlinLogging

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
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = Duration.ofSeconds(2)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

//    DBOperator.connectOrCreate()

    val logger = KotlinLogging.logger {}
    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
    val playerPropertiesByID = mutableMapOf<Int, PlayerProperties>()

    suspend fun handleHTTPRequestError(call: ApplicationCall, path: String, e: Exception) {
        val errorMessage = "Failed GET $path request from ${call.request.origin.remoteAddress}"
        logger.error(errorMessage, e)

        val errorInfo = e.message ?: "Unknown Error"
        call.respond(
            HttpStatusCode.BadRequest,
            mapOf("type" to "error", "message" to errorInfo)
        )
    }

    logger.info("Server is ready")
    routing {
        staticFiles("", File("static"))

        /** Send all textures */
        get("/api/textures") {
            try {
                val textures = DBOperator.getAllTextures()
                call.response.status(HttpStatusCode.OK)
                call.respond(textures.map { mapOf("id" to it.id.toString(), "url" to it.pathToFile) })
                logger.info("Successful GET /api/textures request from: ${call.request.origin.remoteAddress}")
            } catch (e: Exception) {
                handleHTTPRequestError(call, "/api/textures", e)
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
                logger.info("Successful GET /api/textures/$textureID request from: ${call.request.origin.remoteAddress}")
            } catch (e: Exception) {
                handleHTTPRequestError(call, "/api/textures/$textureID", e)
            }
        }

        /** Get json with some fields of PlayerProperties and new values */
        webSocket("/api/connect") {
            val thisConnection = Connection(this)
            connections += thisConnection
            val id = thisConnection.id
            logger.info("WebSocket connection established with ${call.request.origin.remoteAddress}")

            try {
                send(createSimpleMap())
                playerPropertiesByID.forEach {
                    sendSerialized(Player(it.key, it.value))
                }
                playerPropertiesByID[id] = PlayerProperties(id)
                connections.forEach {
                    it.session.sendSerialized(Player(id, playerPropertiesByID.getValue(id)))
                }
                logger.info("WebSocket messages with information of ${call.request.origin.remoteAddress} sent to ${connections.size} clients")

                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val newProperties = JSONObject(frame.readText())
                    val oldProperties = playerPropertiesByID.getValue(id)
                    playerPropertiesByID[id] = updateProperties(oldProperties, newProperties)
                    connections.forEach {
                        it.session.sendSerialized(Player(id, playerPropertiesByID.getValue(id)))
                    }
                    logger.info("WebSocket messages with information of ${call.request.origin.remoteAddress} sent to ${connections.size} clients")
                }
            } catch (e: Exception) {
                val errorMessage = "Failed websocket /api/connect message from ${call.request.origin.remoteAddress}"
                logger.error(errorMessage, e)
            } finally {
                if (playerPropertiesByID.contains(id)) {
                    val properties = playerPropertiesByID.getValue(id)
                    properties.status = PlayerStatus.DISCONNECTED
                    playerPropertiesByID.remove(id)
                    connections.forEach {
                        it.session.sendSerialized(Player(id, properties))
                    }
                    logger.info("WebSocket messages about closing ${call.request.origin.remoteAddress} sent to ${connections.size} clients")
                }

                connections -= thisConnection
            }
        }
    }
}

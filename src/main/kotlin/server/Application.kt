package server

import db.DBOperator
import db.DBOperator.addUser
import db.DBOperator.removePlayerFromSession
import db.UserInfo
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
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.util.*

import mu.KotlinLogging
import org.json.JSONObject

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

    DBOperator.connectOrCreate()

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
            val textureID = call.parameters["id"]?.toUIntOrNull() ?: 0u
            try {
                val textureFile = File(
                    DBOperator.getTextureByID(textureID)?.pathToFile
                        ?: throw IllegalArgumentException("Texture #$textureID does not exist")
                )
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

        post("/api/register") {
            val reg = call.parameters["register"]?.toIntOrNull() ?: 0
            logger.info("WebSocket messages with information of /api/textures/$reg request from: ${call.request.origin.remoteAddress}")
            val parameters = call.receiveParameters()
            val login = parameters["login"]
            val password = parameters["password"]
            val email = parameters["email"]

            if (login != null && email != null && password != null) {
                try {
                    val id =  addUser(login, email, password)
                    call.respond(HttpStatusCode.Created, mapOf("message" to "User $id registered successfully"))
                    logger.info("Successful POST /api/register request from: ${call.request.origin.remoteAddress}")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "User already exists"))
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid request parameters"))
                logger.info("Bad POST /api/register request from: ${call.request.origin.remoteAddress}")
            }
        }
        get("api/users"){
            try {
                val users = DBOperator.getAllUsers()
                call.response.status(HttpStatusCode.OK)
                call.respond(users.map { mapOf("email" to it.login) })
                logger.info("Successful GET /api/users request from: ${call.request.origin.remoteAddress}")
            } catch (e: Exception) {
                handleHTTPRequestError(call, "/api/users", e)
            }
        }

        post("/api/login") {
            val parameters = call.receiveParameters()
            val login = parameters["login"]
            val email = parameters["email"]
            val password = parameters["password"]

            if (password != null) {
                var user: UserInfo? = null

                if (login != null) {
                    user = DBOperator.getUserByLogin(login)
                }

                if (email != null) {
                    user = DBOperator.getUserByEmail(email)
                }

                if (user != null) {
                    val isMatch = DBOperator.checkUserPassword(userId = user.id, password)
                    if (isMatch) {
                        call.respond(HttpStatusCode.OK, mapOf("userId" to user.id))
                    }
                    else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid login/email or password"))
                    }
                }
                else {
                    call.respond(HttpStatusCode.NotFound, mapOf("userId" to -1))
                }

                logger.info("Successful POST /api/login request from: ${call.request.origin.remoteAddress}")
            } else {
                logger.info("Bad POST /api/login request from: ${call.request.origin.remoteAddress}")
            }
        }


        post("/api/logout") {
            val parameters = call.receiveParameters()
            val sessionId = parameters["sessionId"]?.toUIntOrNull()
            val userId = parameters["userId"]?.toUIntOrNull()

            if (sessionId != null && userId != null) {
                removePlayerFromSession(sessionId, userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Logout successful"))
                logger.info("Successful POST /api/logout request from: ${call.request.origin.remoteAddress}")
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid request parameters"))
                logger.info("Bad POST /api/logout request from: ${call.request.origin.remoteAddress}")
            }
        }

        post("/api/edit/{id}") {
            val userId = call.parameters["id"]?.toUIntOrNull()
            val parameters = call.receiveParameters()
            val login = parameters["login"]
            val email = parameters["email"]
            val password = parameters["password"]
            try {
                if (userId != null) {
                    if (login != null) {
                        logger.info(login)
                        DBOperator.updateUserLogin(userId, login)
                        logger.debug("Login for User $userId edit successfully")
                    }
                    if (email != null) {
                        logger.info(login)
                        DBOperator.updateUserEmail(userId, email)
                        logger.debug("Email for User $userId edit successfully")
                    }
                    if (password != null) {
                        logger.info(login)
                        DBOperator.updateUserPassword(userId, password)
                        logger.debug("Password for User $userId edit successfully")
                    }
                    call.respond(HttpStatusCode.Created, mapOf("message" to "Data for User $userId edit successfully"))
                    logger.info("Successful POST /api/edit/{id} request from: ${call.request.origin.remoteAddress}")
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid request parameter 'id'"))
                    logger.info("Bad POST /api/logout request from: ${call.request.origin.remoteAddress}")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Data for User $userId edit failed"))
                logger.info("Failed POST /api/edit/{id} request from: ${call.request.origin.remoteAddress}")
            }
        }
    }
}

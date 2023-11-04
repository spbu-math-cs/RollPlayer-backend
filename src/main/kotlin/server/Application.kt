package server

import db.CharacterInfo
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
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashSet

import mu.KotlinLogging
import org.json.JSONException
import org.json.JSONObject

data class SessionData(
    val connections: MutableList<Connection> = Collections.synchronizedList(mutableListOf()),
    val characters: MutableSet<UInt> = Collections.synchronizedSet(LinkedHashSet()),
    var whoCanMove: AtomicInteger = AtomicInteger(0)
)

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

    val logger = KotlinLogging.logger {}
    val sessions = Collections.synchronizedMap<UInt, SessionData>(mutableMapOf())

    // Мне кажется, лучше не хранить список персонажей,
    // а обращаться к БД всякий раз, когда понадобятся
    // конкретные персонажи.
    // val charactersByID = Collections.synchronizedMap<UInt, Character>(mutableMapOf())

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
        conn: WebSocketServerSession,
        userId: UInt,
        on: String,
        e: Exception
    ) {
        logger.info("Failed websocket message type $on from user with ID $userId (${conn.call.request.origin.remoteAddress})", e)
        conn.send(JSONObject(mapOf("type" to "error", "on" to on, "message" to e.message.orEmpty())).toString())
    }

    // Вообще у меня есть сомнения в правильности этого метода.
    // Если два персонажа одного игрока встанут на одну клетку,
    // то один исчезнет.
    fun getCharactersByUserSession(userId: UInt, sessionId: UInt): Set<CharacterInfo> {
        return DBOperator.getAllPlayerCharactersOfUserInSession(userId, sessionId)
            .toSet()
    }

    suspend fun addCharacterToConn(character: CharacterInfo, conn: WebSocketServerSession, own: Boolean) {
        val characterInfo = JSONObject(Json.encodeToString(character))
        characterInfo.put("type", "character:new")
        characterInfo.put("own", own)
        conn.send(characterInfo.toString())
    }

    // параметр sessionId не нужен, он зашит в CharacterInfo
    suspend fun addNewCharacter(character: CharacterInfo,
                                connId: Int,
                                conn: WebSocketServerSession) {
        // персонаж добавляется в БД раньше
        // val character = DBOperator.addPlayerCharacter(userId, sessionId, x, y)

        val characterJson = JSONObject(Json.encodeToString(character))
        characterJson.put("type", "character:new")
        characterJson.put("own", true)
        conn.send(characterJson.toString())
        characterJson.put("own", false)
        sessions.getValue(character.sessionId).connections.forEach {
            if (it.id != connId) {
                it.session.send(characterJson.toString())
            }
        }
        logger.info("WebSocket: sent new character with ID ${character.id}")
    }

    suspend fun deleteCharacterFromSession(characterId: UInt, sessionId: UInt) {
        sessions.getValue(sessionId).characters.remove(characterId)

        val message = JSONObject()
        message.put("type", "character:leave")
        message.put("id", characterId)
        sessions.getValue(sessionId).connections.forEach { it.session.send(message.toString()) }
        logger.info("WebSocket: sent leaving character with ID $characterId")
    }

    suspend fun moveCharacter(character: CharacterInfo,
                              newRow: Int,
                              newCol: Int) {
        val message = JSONObject()
        message.put("type", "character:move")
        message.put("id", character.id)
        message.put("row", newRow)
        message.put("col", newCol)
        sessions.getValue(character.sessionId).connections.forEach { it.session.send(message.toString()) }
        logger.info("WebSocket: sent move character with ID ${character.id}")
    }

    DBOperator.connectOrCreate()

    logger.info("Server is ready")
    routing {
        staticFiles("", File("static"))

        webSocket("/api/connect/{userId}/{sessionId}") {
            val userIdPrev = call.parameters["userId"]?.toUIntOrNull()
            val sessionIdPrev = call.parameters["sessionId"]?.toUIntOrNull()
            if (userIdPrev == null || sessionIdPrev == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid userId or sessionId: must be UInt"))
            }

            val userId = userIdPrev!!
            val sessionId = sessionIdPrev!!
            if (!sessions.contains(sessionId)) {
                sessions[sessionId] = SessionData()
            }
            val session = sessions.getValue(sessionId)

            val thisConnection = Connection(this)
            session.connections.add(thisConnection)
            val connId = thisConnection.id
            logger.info("WebSocket connection established with ${call.request.origin.remoteAddress}")

            if (session.connections.size == 1) {
                session.whoCanMove = AtomicInteger(connId)
            }

            try {
                send(createSimpleMap())
                session.characters.forEach {
                    addCharacterToConn(
                        DBOperator.getPlayerCharacterByID(it)
                            ?: throw IllegalArgumentException("Character #$it not found in the database"),
                        this, false)
                }
                logger.info("WebSocket: sent active characters in session $sessionId to $userId")
                for (character in getCharactersByUserSession(userId, sessionId)) { // get from db
                    addNewCharacter(
                        character,
                        connId,
                        this)
                }

                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val frameText = frame.readText()
                    val message = JSONObject(frameText)
                    if (!message.has("type")) throw Exception("Invalid websocket message: missing field \"type\"")
                    when (message.getString("type")) {
                        "character:new" -> {
                            try {
                                val characterName = message.optString("name", "Dovakin")
                                val characterRow = message.optInt("row", 1)
                                val characterCol = message.optInt("col", 1)

                                // Примечание: ID персонажа автоматически генерируется БД.
                                // Генерировать его вручную не нужно.
                                // val characterId = (LocalDateTime.now().hour.toString() +
                                //         LocalDateTime.now().minute.toString() +
                                //         LocalDateTime.now().second.toString()).toUInt()
                                val character = DBOperator.addPlayerCharacter(
                                    userId,
                                    sessionId,
                                    characterName,
                                    characterRow,
                                    characterCol)
                                    // CharacterInfo(characterId, userId, sessionId, characterName, characterRow, characterCol)

                                addNewCharacter(character, connId, this)
                            } catch (e: Exception) {
                                handleWebsocketIncorrectMessage(this, userId, "character:new", e)
                            }
                        }
                        "character:remove" -> {
                            try {
                                val characterId = message.getInt("id").toUInt()

                                val character = DBOperator.getPlayerCharacterByID(characterId)
                                    ?: throw Exception("Character with ID $characterId does not exist")
                                if (character.userId != userId || character.sessionId != sessionId)
                                    throw Exception("Character with ID $characterId doesn't belong to you")
                                DBOperator.deletePlayerCharacterByID(character.id)

                                deleteCharacterFromSession(characterId, sessionId)
                            } catch (e: Exception) {
                                handleWebsocketIncorrectMessage(this, userId, "character:remove", e)
                            }
                        }
                        "character:move" -> {
                            try {
                                val characterId = message.getInt("id").toUInt()

                                val character = DBOperator.getPlayerCharacterByID(characterId)
                                    ?: throw Exception("Character with ID $characterId does not exist")
                                if (character.userId != userId || character.sessionId != sessionId)
                                    throw Exception("Character with ID $characterId doesn't belong to you")
                                val newRow = message.getInt("row")
                                val newCol = message.getInt("col")

                                DBOperator.movePlayerCharacter(
                                    character.id,
                                    newRow, newCol)

                                if (connId != session.whoCanMove.get())
                                    throw Exception("Can not move")
                                moveCharacter(character, newRow, newCol)

                                // TODO: Здесь небезопасная ерунда написана, я знаю, но пока так.
                                val cur = session.connections.indexOfFirst { it.id == session.whoCanMove.get() }
                                val messageStatus = JSONObject()
                                messageStatus.put("type", "character:status")
                                messageStatus.put("can_move", false)
                                session.connections[cur].session.send(messageStatus.toString())
                                val next = if (cur < session.connections.size - 1) cur + 1 else 0
                                session.whoCanMove = AtomicInteger(session.connections[next].id)
                                messageStatus.put("can_move", true)
                                session.connections[next].session.send(messageStatus.toString())
                            } catch (e: Exception) {
                                handleWebsocketIncorrectMessage(this, userId, "character:move", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                handleWebsocketIncorrectMessage(this, userId, "", e)
            } finally {
                session.characters
                    .filter { DBOperator.getUserByID(userId)?.id == userId }
                    .forEach { deleteCharacterFromSession(it, sessionId) }
                session.connections.remove(thisConnection)

                if (session.connections.isEmpty()) {
                    sessions.remove(sessionId)
                }
            }
        }

        get("/api/textures") {
            try {
                val textures = DBOperator.getAllTextures()
                call.response.status(HttpStatusCode.OK)
                call.respond(textures.map { mapOf("id" to it.id.toString(), "url" to it.pathToFile) })
                logger.info("Successful GET /api/textures request from: ${call.request.origin.remoteAddress}")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "GET /api/textures", e)
            }
        }

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
                handleHTTPRequestException(call, "/api/textures/$textureID", e)
            }
        }

        post("/api/register") {
            try {
                val data = JSONObject(call.receiveText())
                val login = data.getString("login")
                val email = data.getString("email")
                val password = data.getString("password")

                val id = DBOperator.addUser(login, email, password)
                call.respond(HttpStatusCode.Created, mapOf(
                    "message" to "User $id registered successfully",
                    "userInfo" to Json.encodeToString(DBOperator.getUserByID(id)!!)
                ))
                logger.info("Successful POST /api/register request from: ${call.request.origin.remoteAddress}")
            } catch (e: JSONException) {
                handleHTTPRequestException(call, "POST /api/register", e, "Invalid body for request POST /api/register")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "POST /api/register", e)
            }
        }

        get("/api/users"){
            try {
                val users = DBOperator.getAllUsers()
                call.response.status(HttpStatusCode.OK)
                call.respond(users.map { mapOf("login" to it.login, "email" to it.email) })
                logger.info("Successful GET /api/users request from: ${call.request.origin.remoteAddress}")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "GET /api/users", e)
            }
        }

        post("/api/login") {
            try {
                val data = JSONObject(call.receiveText())
                val password = data.getString("password")

                val user = if (data.has("login")) {
                    DBOperator.getUserByLogin(data.getString("login"))
                } else if (data.has("email")) {
                    DBOperator.getUserByEmail(data.getString("email"))
                } else throw Exception("Invalid request POST /api/login: missing login and email")

                if (user != null) {
                    if (DBOperator.checkUserPassword(userId = user.id, password)) {
                        call.respond(HttpStatusCode.OK, mapOf("userId" to user.id))
                    } else throw Exception("Invalid request POST /api/login: invalid login/email or password")
                }
                else {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "User with this login/email is not exist"))
                }
                logger.info("Successful POST /api/login request from: ${call.request.origin.remoteAddress}")
            } catch (e: JSONException) {
                handleHTTPRequestException(call, "POST /api/login", e, "Invalid body for request POST /api/login")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "POST /api/login", e)
            }
        }

        post("/api/logout") {
            try {
                val data = JSONObject(call.receiveText())
                val userId = data.getInt("userId").toUInt()

//                DBOperator.removeAllPlayerCharactersOfUserFromSession(sessionId, userId)
//                if (data.has("sessionId")) {
//                    val sessionId = data.getString("sessionId").toUInt()
//                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Logout successful"))
                logger.info("Successful POST /api/logout request from: ${call.request.origin.remoteAddress}")
            } catch (e: JSONException) {
                handleHTTPRequestException(call, "POST /api/logout", e, "Invalid body for request POST /api/logout")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "POST /api/logout", e)
            }
        }

        post("/api/edit/{userId}") {
            try {
                val userId = call.parameters["userId"]?.toUIntOrNull()
                    ?: throw Exception("Invalid request POST /api/edit/{userId}: invalid userId, must be UInt")

                val data = JSONObject(call.receiveText())
                if (data.has("login")) {
                    val newLogin = data.getString("login")
                    DBOperator.updateUserLogin(userId, newLogin)
                    logger.info("Login for User $userId edit successfully")
                }
                if (data.has("email")) {
                    val newEmail = data.getString("email")
                    DBOperator.updateUserEmail(userId, newEmail)
                    logger.info("Email for User $userId edit successfully")
                }
                if (data.has("password")) {
                    val newPassword = data.getString("password")
                    DBOperator.updateUserPassword(userId, newPassword)
                    logger.info("Password for User $userId edit successfully")
                }

                call.respond(HttpStatusCode.Created, mapOf(
                    "message" to "Data for User $userId edit successfully",
                    "userInfo" to Json.encodeToString(DBOperator.getUserByID(userId)!!)
                ))
                logger.info("Successful POST /api/edit/{userId} request from: ${call.request.origin.remoteAddress}")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "POST /api/edit/{userId}", e)
            }
        }
    }
}

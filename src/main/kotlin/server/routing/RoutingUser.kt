package server.routing

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import db.DBOperator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import server.JWTParams
import server.logger
import server.utils.handleHTTPRequestException
import java.util.*

fun Route.requestsUser(jwtParams: JWTParams){
    get("/api/users"){
        try {
            val users = DBOperator.getAllUsers()
            call.respond(HttpStatusCode.OK, JSONObject()
                .put("type", "ok")
                .put("result", JSONArray(users.map { JSONObject(Json.encodeToString(it)) }))
                .toString()
            )
            logger.info("Successful GET /api/users request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "GET /api/users", e)
        }
    }

    post("/api/register") {
        try {
            val data = JSONObject(call.receiveText())
            val login = data.getString("login")
            val email = data.getString("email")
            val password = data.getString("password")
            val avatarId = if (data.has("avatarId")) {
                data.getInt("avatarId").toUInt()
            } else {
                null
            }

            val userInfo = DBOperator.addUser(login, email, password, avatarId)
            call.respond(HttpStatusCode.Created, JSONObject()
                .put("type", "ok")
                .put("message", "User ${userInfo.id} registered successfully")
                .put("result", JSONObject(Json.encodeToString(userInfo)))
                .toString()
            )

            logger.info("Successful POST /api/register request from: ${call.request.origin.remoteAddress}")
        } catch (e: JSONException) {
            handleHTTPRequestException(call, "POST /api/register", e, "Invalid body for request POST /api/register")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "POST /api/register", e)
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
            } else throw Exception("Missing login and email")

            if (user != null) {
                if (DBOperator.checkUserPassword(user.id, password)) {
                    val token = JWT.create()
                        .withAudience(jwtParams.audience)
                        .withIssuer(jwtParams.issuer)
                        .withClaim("id", user.id.toLong())
                        .withClaim("login", user.login)
                        .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                        .sign(Algorithm.HMAC256(jwtParams.secret))

                    call.respond(HttpStatusCode.OK, JSONObject()
                        .put("type", "ok")
                        .put("message", "User ${user.id} logged in successfully")
                        .put("result", token)
                        .toString()
                    )
                } else throw Exception("Invalid login/email or password")
            } else throw Exception("User with this login/email is not exist")

            logger.info("Successful POST /api/login request from: ${call.request.origin.remoteAddress}")
        } catch (e: JSONException) {
            handleHTTPRequestException(call, "POST /api/login", e, "Invalid body for request POST /api/login")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "POST /api/login", e)
        }
    }

    authenticate("auth-jwt") {
        post("/api/logout") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asInt().toUInt()

            try {
                call.respond(HttpStatusCode.OK, JSONObject()
                    .put("type", "ok")
                    .put("message", "User $userId logged out successfully")
                    .toString()
                )
                logger.info("Successful POST /api/logout request from: ${call.request.origin.remoteAddress}")
            } catch (e: JSONException) {
                handleHTTPRequestException(call, "POST /api/logout", e, "Invalid body for request POST /api/logout")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "POST /api/logout", e)
            }
        }

        get("/api/user") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asInt().toUInt()

            try {
                call.respond(
                    HttpStatusCode.OK, JSONObject()
                        .put("type", "ok")
                        .put("message", "Get userdata successfully")
                        .put("result", JSONObject(Json.encodeToString(DBOperator.getUserByID(userId)!!)))
                        .toString()
                )
                logger.info("Successful GET /api/user request from: ${call.request.origin.remoteAddress}")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "GET /api/user", e)
            }
        }

        post("/api/user/edit") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asInt().toUInt()

            try {
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
                if (data.has("avatarId")) {
                    if (data.isNull("avatarId")) {
                        DBOperator.updateUserAvatar(userId, null)
                    } else {
                        val newAvatarId = data.getInt("avatarId").toUInt()
                        DBOperator.updateUserAvatar(userId, newAvatarId)
                    }
                    logger.info("Avatar for User $userId edit successfully")
                }

                call.respond(
                    HttpStatusCode.OK, JSONObject()
                        .put("type", "ok")
                        .put("message", "Data for user $userId edit successfully")
                        .put("result", JSONObject(Json.encodeToString(DBOperator.getUserByID(userId)!!)))
                        .toString()
                )

                logger.info("Successful POST /api/user/edit request from: ${call.request.origin.remoteAddress}")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "POST /api/user/edit", e)
            }
        }

        get("/api/user/sessions") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asInt().toUInt()

            try {
                call.respond(HttpStatusCode.OK, JSONObject()
                    .put("type", "ok")
                    .put("result", JSONArray(DBOperator.getAllSessionsWithUser(userId).map { JSONObject(Json.encodeToString(it)) }))
                    .toString()
                )
                logger.info("Successful GET /api/user/sessions request from: ${call.request.origin.remoteAddress}")
            } catch (e: Exception) {
                handleHTTPRequestException(call, "GET /api/user/sessions", e)
            }
        }
    }
}

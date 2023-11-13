package server.routing

import server.*
import db.DBOperator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun Route.requestsUser(){
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
                    call.respond(HttpStatusCode.OK, mapOf(
                        "message" to "User ${user.id} logged in successfully",
                        "userInfo" to Json.encodeToString(user)
                    ))
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

            call.respond(
                HttpStatusCode.OK, mapOf(
                "message" to "Data for User $userId edit successfully",
                "userInfo" to Json.encodeToString(DBOperator.getUserByID(userId)!!)
            ))
            logger.info("Successful POST /api/edit/{userId} request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "POST /api/edit/{userId}", e)
        }
    }

    get("/api/{userId}/sessions") {
        try {
            val userId = call.parameters["userId"]?.toUIntOrNull()
                ?: throw Exception("Invalid request GET /api/edit/{userId}: invalid userId, must be UInt")

            call.respond(
                HttpStatusCode.OK,
                JSONArray(DBOperator.getAllSessionsWithUser(userId).map { Json.encodeToString(it) }).toString()
            )
            logger.info("Successful GET /api/{userId}/sessions request from: ${call.request.origin.remoteAddress}")
        } catch (e: Exception) {
            handleHTTPRequestException(call, "GET /api/{userId}/sessions", e)
        }
    }
}

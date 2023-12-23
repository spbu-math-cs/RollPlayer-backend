import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.config.ConfigFactory
import db.DBOperator
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import server.ActiveSessionData
import server.JWTParams
import server.routing.*
import java.io.File
import java.time.Duration
import java.util.*
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.withApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.time.ExperimentalTime

private fun createErrorResponseMessage(msg: String?) = mapOf(
    "type" to "error", "message" to msg
).toString()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpsTest {

    private lateinit var engine: TestApplicationEngine

    @BeforeAll
    fun setUp() {
        val serverAddress = "http://127.0.0.1:1234"
        embeddedServer(Netty, port = 1234) {

        }.start(wait = true)
    }

    @AfterAll
    fun tearDown() {
        engine.stop(1000, 2000)
    }


    @Test
    fun `GET request to api-textures returns expected response`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:1234/api/textures")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.body<String>()
        assertEquals(
            "{\"result\":[{\"filepath\":\".\\\\resources\\\\textures\\\\tileset_packed.png\",\"id\":\"1\"},{\"filepath\":\".\\\\resources\\\\textures\\\\tileset_packed_plus.png\",\"id\":\"2\"}],\"type\":\"ok\"}",
            responseBody
        )
    }

    @Test
    fun `GET request to api-textures-id returns expected response`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/textures/1")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET request with non-cast-to-UInt to api-textures-id returns expected response`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/textures/1ewd")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"type\":\"error\",\"message\":\"Texture #0 does not exist\"}",
            responseBody
        )
    }

    @Test
    fun `GET request to non-existing api-textures-id returns 400 error`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/textures/999")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"type\":\"error\",\"message\":\"Texture #999 does not exist\"}",
            responseBody
        )
    }

    @Test
    fun `GET request to api-tilesets returns expected response`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/tilesets")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"result\":[{\"filepath\":\".\\\\resources\\\\tilesets\\\\tileset_packed.tsj\",\"id\":\"1\"},{\"filepath\":\".\\\\resources\\\\tilesets\\\\tileset_packed_plus.tsj\",\"id\":\"2\"}],\"type\":\"ok\"}",
            responseBody
        )
    }

    @Test
    fun `GET request to api-tilesets-id returns expected response`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/tilesets/1")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"columns\": 49, \"image\": \"tileset_packed.png\", \"imageheight\": 352, \"imagewidth\": 784, \"margin\": 0, \"name\": \"tileset_packed\", \"spacing\": 0, \"tilecount\": 1078, \"tiledversion\": \"1.10.2\", \"tileheight\": 16, \"tilewidth\": 16, \"type\": \"tileset\", \"version\": \"1.10\"}".filter { !it.isWhitespace() },
            responseBody.filter { !it.isWhitespace() }
        )
    }

    @Test
    fun `GET request to non-existing api-tilesets returns 400 error`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/tilesets/100")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"type\":\"error\",\"message\":\"Tileset #100 does not exist\"}",
            responseBody
        )
    }

    @Test
    fun `GET request to api-maps returns expected response`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/maps")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"result\":[{\"filepath\":\".\\\\resources\\\\maps\\\\example_map.tmj\",\"id\":\"1\"},{\"filepath\":\".\\\\resources\\\\maps\\\\example_map_with_obstacles.tmj\",\"id\":\"2\"},{\"filepath\":\".\\\\resources\\\\maps\\\\forest_ruins.tmj\",\"id\":\"3\"}],\"type\":\"ok\"}",
            responseBody
        )
    }

    @Test
    fun `GET request to api-maps-id returns expected response`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/maps/1")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            File("./resources/maps/example_map.tmj").readText(),
            responseBody
        )
    }

    @Test
    fun `GET request to api-maps returns expected response returns error`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/maps/999")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"type\":\"error\",\"message\":\"Map #999 does not exist\"}",
            responseBody
        )
    }

    @Test
    fun `GET request to api-users returns expected response`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/users")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"result\":[{\"avatarID\":null,\"id\":1,\"login\":\"newLogin\",\"email\":\"test1@email.ru\",\"passwordHash\":1173239797},{\"avatarID\":null,\"id\":2,\"login\":\"testLogin999\",\"email\":\"test999@email.ru\",\"passwordHash\":1190994742},{\"avatarID\":null,\"id\":3,\"login\":\"testLogin\",\"email\":\"test@email.ru\",\"passwordHash\":885373811},{\"avatarID\":null,\"id\":4,\"login\":\"testLogin9999\",\"email\":\"test9999@email.ru\",\"passwordHash\":986307396}],\"type\":\"ok\"}",
            responseBody
        )
    }

    // проходит нормально ровно один раз - когда впервые запускаешь и юзер создается
//    @Test
//    fun `POST request to api-register returns expected response`(): Unit = runBlocking {
//        val requestBody = """{"login": "testLogin9999", "email": "test9999@email.ru", "password": "test9999Password"}"""
//        val response = HttpClient().post("http://127.0.0.1:9999/api/register"){
//            setBody(requestBody)
//        }
//        assertEquals(HttpStatusCode.Created, response.status)
//        val responseBody: String = response.bodyAsText()
//        assertEquals(
//            "{\"result\":{\"avatarID\":null,\"id\":4,\"login\":\"testLogin9999\",\"email\":\"test9999@email.ru\",\"passwordHash\":986307396},\"type\":\"ok\",\"message\":\"User 4 registered successfully\"}",
//            responseBody
//        )
//    }

    @Test
    fun `POST request with incorrect password to api-register returns 400 error`(): Unit = runBlocking {
        val requestBody = """{"login": "testLogin", "email": "incorrectEmail", "password": "pass"}"""
        val response = HttpClient().post("http://127.0.0.1:9999/api/register"){
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"type\":\"error\",\"message\":\"User with login `testLogin` already exists\"}",
            responseBody
        )
    }

    @Test
    fun `POST request with existing email to api-register returns 400 error`(): Unit = runBlocking {
        try {
            val requestBody = """{"login": "testLogin", "email": "test@email.ru", "password": "testPassword"}"""
            val response = HttpClient().post("http://127.0.0.1:9999/api/register") {
                setBody(requestBody)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody: String = response.bodyAsText()
            assertEquals(
                "{\"type\":\"error\",\"message\":\"User with login `testLogin` already exists\"}",
                responseBody
            )
        }
        finally {

        }

    }

    @Test
    fun `POST request with incorrect email to api-register returns 400 error`(): Unit = runBlocking {
        val requestBody = """{"login": "testLogin", "email": "incorrectEmail", "password": "testPassword"}"""
        val response = HttpClient().post("http://127.0.0.1:9999/api/register"){
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"type\":\"error\",\"message\":\"User with login `testLogin` already exists\"}",
            responseBody
        )
    }

    @Test
    fun `POST request to api-login returns expected response`(): Unit = runBlocking {
        val requestBody = """{"login": "testLogin", "password": "testPassword"}"""
        val response = HttpClient().post("http://127.0.0.1:9999/api/login"){
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "User 3 logged in successfully",
            JSONObject(response.bodyAsText()).getString("message")
        )
    }

    @Test
    fun `POST request to non-existing api-login returns 400 error`(): Unit = runBlocking {
        val requestBody = """{"login": "nonExistingLogin", "password": "testPassword"}"""
        val response = HttpClient().post("http://127.0.0.1:9999/api/login"){
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "{\"type\":\"error\",\"message\":\"User with this login/email is not exist\"}",
            responseBody
        )
    }

    @Test
    fun `POST request to api-logout returns expected response`(): Unit = runBlocking {
        val loginBody = """{"login": "testLogin", "password": "testPassword"}"""
        val login = HttpClient().post("http://127.0.0.1:9999/api/login"){
            setBody(loginBody)
        }
        assertEquals(HttpStatusCode.OK, login.status)

        val logoutBody = """{"userId": 1}"""
        val logout: HttpResponse = HttpClient().post("http://127.0.0.1:9999/api/logout") {
            setBody(logoutBody)
        }
        assertEquals(HttpStatusCode.Unauthorized, logout.status)
        val logoutT: String = logout.bodyAsText()
        assertEquals(
            "Token is not valid or has expired",
            logoutT
        )
    }

    @Test
    fun `POST request to api-logout returns error`(): Unit = runBlocking {
        val requestBody = """{"userId": 1}"""
        val response = HttpClient().post("http://127.0.0.1:9999/api/logout"){
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val responseBody: String = response.bodyAsText()
        assertEquals(
            "Token is not valid or has expired",
            responseBody
        )
    }

    @Test
    fun `POST request to api-edit-userId returns expected response`(): Unit = runBlocking {
        val loginBody = """{"login": "1Login", "email": "1@email.ru", "password": "test1"}"""
        val login = HttpClient().post("http://127.0.0.1:9999/api/login"){
            setBody(loginBody)
        }

        val requestBody = """{"login": "2Login", "email": "2@email.ru", "password": "test2"}"""
        val response: HttpResponse = HttpClient().post("http://127.0.0.1:9999/api/user/edit") {
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(
            "Token is not valid or has expired",
            response.bodyAsText()
        )
    }



//    @Test
//    fun `GET request to api-userId-sessions returns expected response`(): Unit = runBlocking {
//        val loginBody = """{"login": "testLogin", "password": "testPassword"}"""
//
//        val login = HttpClient().post("http://127.0.0.1:9999/api/login"){
//            setBody(loginBody)
//        }
//        assertEquals(HttpStatusCode.OK, login.status)
//        val requestBody = """{"id": 1}"""
//        val response = HttpClient().post("http://127.0.0.1:9999/api/user/sessions"){
//            setBody(requestBody)
//        }
//        assertEquals(HttpStatusCode.OK, response.status)
//
//        assertEquals(
//            "{\"result\":[],\"type\":\"ok\"}",
//            response.bodyAsText()
//        )
//    }

    @Test
    fun `GET request to api-userId-sessions returns error`(): Unit = runBlocking {
        val requestBody = """{"id": 1}"""

        val response = HttpClient().get("http://127.0.0.1:9999/api/user/sessions") {
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(
            "Token is not valid or has expired",
            response.bodyAsText()
        )
    }

//    @Test
//    fun `POST request to api-game-create returns expected response`(): Unit = runBlocking {
//        val requestBody = MapInfo(1u, "/maps/example_map.tmj")
//
//        val response = HttpClient().post("http://127.0.0.1:999/api/game/create") {
//            setBody(requestBody)
//        }
//        assertEquals(HttpStatusCode.OK, response.status)
//        assertEquals(
//            "{\"type\":\"ok\",\"message\":\"Session created\",\"result\":{\"sessionId\":1,\"mapId\":2,\"active\":true,\"started\":\"${Clock.System.now()}\",\"prevCharacterId\":0}}",
//            response.bodyAsText()
//        )
//    }

    @Test
    fun `POST request to api-game-create without mapId returns 400 error`(): Unit = runBlocking {
        val response: HttpResponse = HttpClient().post("http://127.0.0.1:9999/api/game/create")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"Request must contain \\\"mapId\\\" query parameter\"}",
            response.bodyAsText()
        )
    }

//    @Test
//    fun `GET request to api-game-sessionId-mapId returns expected response`(): Unit = runBlocking {
//        val requestBody = MapInfo(1u, "/path/to/map1.json")
//        val response: HttpResponse = HttpClient().get("http://127.0.0.1:9999/api/game/1/mapId") {
//            setBody(requestBody)
//        }
//
//        assertEquals(HttpStatusCode.NotFound, response.status)
//
//        assertEquals(
//            "",
//            response.bodyAsText()
//        )
//    }

    @Test
    fun `GET request to non-existing api-game-sessionId-mapId returns 400 error`(): Unit = runBlocking {
        val requestBody = """{"id": 999}"""
        val response: HttpResponse = HttpClient().get("http://127.0.0.1:9999/api/game/999/mapId") {
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            "{\"type\":\"error\",\"message\":\"Session #999 does not exist\"}",
            response.bodyAsText()
        )
    }

    @Test
    fun `GET request to api-pictures returns expected response`(): Unit = runBlocking {
        val response: HttpResponse = HttpClient().get("http://127.0.0.1:9999/api/pictures")
        assertEquals(HttpStatusCode.OK, response.status)

//        assertEquals(
//            "{\"result\":{\"filepath\":\"./resources/pictures/img_2023-12-19T21:55:49.105280400.png\",\"id\":\"8\"},\"type\":\"ok\"}",
//            response.bodyAsText()
//        )
    }

    @Test
    fun `GET request to api-pictures-id returns expected response`(): Unit = runBlocking {
        val requestBody = """{"id": 1}"""
        val response: HttpResponse = HttpClient().get("http://127.0.0.1:9999/api/pictures") {
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.OK, response.status)
//        assertEquals(
//            "{\"result\":[{\"filepath\":\"./resources/pictures/img_2023-12-19T21:54:40.758162900.png\",\"id\":\"4\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T21:54:53.159829700.png\",\"id\":\"5\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T21:55:05.662137600.png\",\"id\":\"6\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T21:55:30.200976600.png\",\"id\":\"7\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T21:55:49.105280400.png\",\"id\":\"8\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T22:12:15.016607900.png\",\"id\":\"9\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T22:12:48.981366100.png\",\"id\":\"10\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T22:14:01.303129.png\",\"id\":\"11\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T22:20:59.605302.png\",\"id\":\"12\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T22:26:32.474949400.png\",\"id\":\"13\"},{\"filepath\":\"./resources/pictures/img_2023-12-21T12:03:59.439532700.png\",\"id\":\"14\"},{\"filepath\":\"./resources/pictures/img_2023-12-21T12:12:51.205454400.png\",\"id\":\"15\"},{\"filepath\":\"./resources/pictures/img_2023-12-21T12:29:28.991237.png\",\"id\":\"16\"},{\"filepath\":\"./resources/pictures/img_2023-12-21T12:29:58.590485700.png\",\"id\":\"17\"},{\"filepath\":\"./resources/pictures/img_2023-12-21T12:32:24.953699900.png\",\"id\":\"18\"},{\"filepath\":\".\\\\resources\\\\pictures\\\\avatar01.png\",\"id\":\"1\"},{\"filepath\":\".\\\\resources\\\\pictures\\\\avatar02.png\",\"id\":\"2\"},{\"filepath\":\".\\\\resources\\\\pictures\\\\avatar03.png\",\"id\":\"3\"}],\"type\":\"ok\"}",
//            response.bodyAsText()
//        )

    }

//    @Test
//    fun `GET request to non-existing api-pictures-id returns 400 error`(): Unit = runBlocking {
//        val requestBody = """{"id": -1000}"""
//        val response: HttpResponse = HttpClient().get("http://127.0.0.1:9999/api/pictures") {
//            setBody(requestBody)
//        }
//        assertEquals(HttpStatusCode.OK, response.status)
//        assertEquals(
//            "{\"result\":[{\"filepath\":\"./resources/pictures/img_2023-12-19T21:54:40.758162900.png\",\"id\":\"4\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T21:54:53.159829700.png\",\"id\":\"5\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T21:55:05.662137600.png\",\"id\":\"6\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T21:55:30.200976600.png\",\"id\":\"7\"},{\"filepath\":\"./resources/pictures/img_2023-12-19T21:55:49.105280400.png\",\"id\":\"8\"},{\"filepath\":\".\\\\resources\\\\pictures\\\\avatar01.png\",\"id\":\"1\"},{\"filepath\":\".\\\\resources\\\\pictures\\\\avatar02.png\",\"id\":\"2\"},{\"filepath\":\".\\\\resources\\\\pictures\\\\avatar03.png\",\"id\":\"3\"}],\"type\":\"ok\"}",
//            response.bodyAsText()
//        )
//    }

    @Test
    fun `POST request to api-pictures returns expected response`(): Unit = runBlocking {
        //val requestBody = """{"id": 999}"""
        val requestBody = "0x10, 0x10, 0x01, 0x11, 0x11, 0x11, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff"
        val response: HttpResponse = HttpClient().post("http://127.0.0.1:9999/api/pictures") {
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.OK, response.status)
//        assertEquals(
//            "{\"result\":{\"filepath\":\"./resources/pictures/img_2023-12-21T12:29:28.991237.png\",\"id\":\"16\"},\"type\":\"ok\"}",
//            response.bodyAsText()
//        )
    }

//    @Test
//    fun `GET request to api-user returns expected response`(): Unit = runBlocking {
//        val loginBody = """{"login": "testLogin", "password": "testPassword"}"""
//        val login: HttpResponse = HttpClient().post("/api/login") {
//            setBody(loginBody)
//        }
//        assertEquals(HttpStatusCode.OK, login.status)
//        val requestBody = """{"id": 1}"""
//
//        val response: HttpResponse = HttpClient().get("/api/user") {
//            setBody(requestBody)
//        }
//
//        assertEquals(HttpStatusCode.OK, response.status)
//
//        assertEquals(
//            "{\"result\":{\"avatarID\":null,\"id\":1,\"login\":\"testLogin\",\"email\":\"test@email.ru\",\"passwordHash\":1838241622},\"type\":\"ok\",\"message\":\"Get userdata successfully\"}",
//            response.bodyAsText()
//        )
//    }


//    @Test
//    fun `GET request to api-user returns error`(): Unit = runBlocking {
//        val requestBody = """{"id": 1}"""
//
//        val response: HttpResponse = HttpClient().get("/api/user")
//
//        assertEquals(HttpStatusCode.Unauthorized, response.status)
//    }
//

}
import io.ktor.server.netty.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.h2.engine.Database
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.io.File
import org.junit.jupiter.api.Assertions.*

private fun createErrorResponseMessage(msg: String?) = mapOf(
    "type" to "error", "message" to msg
).toString()

class HttpsTest {
    fun main(args: Array<String>): Unit = EngineMain.main(args)
    @Test
    fun `GET request to api-textures returns expected response`(): Unit = runBlocking {
        val response = HttpClient().get("http://127.0.0.1:9999/api/textures")
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
        val responseBody: ByteArray = response.body()
//        assertEquals(
//            "[B@10641c09",
//            responseBody.toString()
//        )
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

//    @Test
//    fun `GET request to api-tilesets-id returns expected response`(): Unit = runBlocking {
//        val response = HttpClient().get("http://127.0.0.1:9999/api/tilesets/1")
//        assertEquals(HttpStatusCode.OK, response.status)
//        val responseBody = response
//        println (responseBody)
//        assertEquals(
//            """{
//        "columns": 49,
//        "image": "tileset_packed.png",
//        "imageheight": 352,
//        "imagewidth": 784,
//        "margin": 0,
//        "name": "tileset_packed",
//        "spacing": 0,
//        "tilecount": 1078,
//        "tiledversion": "1.10.2",
//        "tileheight": 16,
//        "tilewidth": 16,
//        "type": "tileset",
//        "version": "1.10"
//    }""",
//            responseBody
//        )
//    }

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
            "{\"result\":[{\"avatarID\":null,\"id\":1,\"login\":\"newLogin\",\"email\":\"test1@email.ru\",\"passwordHash\":1173239797},{\"avatarID\":null,\"id\":2,\"login\":\"testLogin999\",\"email\":\"test999@email.ru\",\"passwordHash\":1190994742},{\"avatarID\":null,\"id\":3,\"login\":\"testLogin\",\"email\":\"test@email.ru\",\"passwordHash\":885373811}],\"type\":\"ok\"}",
            responseBody
        )
    }

//    @Test
//    fun `POST request to api-register returns expected response`(): Unit = runBlocking {
//        val requestBody = """{"login": "testLogin999", "email": "test999@email.ru", "password": "test999Password"}"""
//        val response = HttpClient().post("http://127.0.0.1:9999/api/register"){
//            setBody(requestBody)
//        }
//        assertEquals(HttpStatusCode.Created, response.status)
//        val responseBody: String = response.bodyAsText()
//        assertEquals(
//            "{\"result\":{\"avatarID\":null,\"id\":2,\"login\":\"testLogin999\",\"email\":\"test999@email.ru\",\"passwordHash\":1190994742},\"type\":\"ok\",\"message\":\"User 2 registered successfully\"}",
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
        val requestBody = """{"login": "testLogin", "email": "test@email.ru", "password": "testPassword"}"""
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
//        val loginT: String = login.bodyAsText()
//        assertEquals(
//            "{\"result\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJodHRwOi8vZXhhbXBsZS5jb20vaGVsbG8iLCJpc3MiOiJodHRwOi8vZXhhbXBsZS5jb20vIiwiaWQiOjEsImxvZ2luIjoidGVzdExvZ2luIiwiZXhwIjoxNzAzMDc2NjE0fQ._Qas1wWjBSFiSNQf_7lrBPlc7rGhk0GjxgYjqblp4c0\",\"type\":\"ok\",\"message\":\"User 1 logged in successfully\"}",
//            loginT
//        )
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
        //assertEquals(HttpStatusCode.OK, login.status)
        //val token = JSONObject(login.bodyAsText()).get("result").toString()

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
//    fun `POST request to api-edit-userId without JWT returns error`(): Unit = runBlocking {
//        val loginBody = """{"login": "testLogin", "password": "testPassword"}"""
//        val login = HttpClient().post("http://127.0.0.1:9999/api/login"){
//            setBody(loginBody)
//        }
//        val token = JSONObject(login.bodyAsText()).get("result").toString()
//
//        val requestBody = """{"login": "newLogin", "email": "test1@email.ru", "password": "testPassword"}"""
//
//        val response: HttpResponse = HttpClient().post("/api/user/edit") {
//            headers["Authorization"] = "Bearer $token"
//            setBody(requestBody)
//        }
//        assertEquals(HttpStatusCode.OK, response.status)
//
//        assertEquals(
//            "Token is not valid or has expired",
//            response.bodyAsText()
//        )
//    }


//
//    @Test
//    fun `GET request to api-userId-sessions returns expected response`() = testApplication {
//
//        mockk<DBOperator> {
//            every { getAllSessionsWithUser(any()) } returns emptyList()
//        }
//
//        val loginBody = """{"login": "testLogin", "password": "testPassword"}"""
//
//        val login: HttpResponse = client.post("/api/login") {
//            setBody(loginBody)
//        }
//
//        val token = JSONObject(login.bodyAsText()).get("result").toString()
//
//        val requestBody = """{"id": 1}"""
//
//        val response = client.get("/api/user/sessions") {
//            headers["Authorization"] = "Bearer $token"
//            setBody(requestBody)
//        }
//
//        assertEquals(HttpStatusCode.OK, response.status)
//
//        assertEquals("{\"result\":[],\"type\":\"ok\"}", response.bodyAsText())
//    }
//
//
//    @Test
//    fun `GET request to api-userId-sessions returns error`() = testApplication {
//        mockk<DBOperator> {
//            every { getAllSessionsWithUser(any()) } returns emptyList()
//        }
//
//        val requestBody = """{"id": 1}"""
//
//        val response = client.get("/api/user/sessions") {
//            setBody(requestBody)
//        }
//
//        assertEquals(HttpStatusCode.Unauthorized, response.status)
//    }
//
//
//    @Test
//    fun `POST request to api-game-create returns expected response`() = testApplication {
//        mockk<DBOperator> {
//            every { addSession(any()) } returns SessionInfo(1u, 2u, true, Clock.System.now(), 0)
//        }
//        val response: HttpResponse = client.post("/api/game/create?mapId=2")
//        assertEquals(HttpStatusCode.OK, response.status)
//        assertEquals(
//            "{\"type\":\"ok\",\"message\":\"Session created\",\"result\":{\"sessionId\":1,\"mapId\":2,\"active\":true,\"started\":\"${Clock.System.now()}\",\"prevCharacterId\":0}}",
//            response.bodyAsText()
//        )
//    }
//
//    @Test
//    fun `POST request to api-game-create without mapId returns 400 error`() = testApplication {
//        mockk<DBOperator> {}
//
//        val response: HttpResponse = client.post("/api/game/create")
//
//        assertEquals(HttpStatusCode.BadRequest, response.status)
//
//        assertEquals(
//            "{\"type\":\"error\",\"message\":\"Request must contain \\\"mapId\\\" query parameter\"}",
//            response.bodyAsText()
//        )
//    }
//
//    @Test
//    fun `GET request to api-game-sessionId-mapId returns expected response`() = testApplication {
//        mockk<DBOperator> {
//            every { getSessionByID(any()) } returns SessionInfo(1u, 2u, true, Clock.System.now(), 0)
//        }
//
//        val response: HttpResponse = client.get("/api/game/1/mapId")
//
//        assertEquals(HttpStatusCode.OK, response.status)
//
//        assertEquals(
//            "{\"type\":\"ok\",\"result\":{\"id\":1,\"mapID\":2,\"active\":true,\"started\":\"${Clock.System.now()}\",\"prevCharacterId\":0}}",
//            response.bodyAsText()
//        )
//    }
//
//    @Test
//    fun `GET request to non-existing api-game-sessionId-mapId returns 400 error`() = testApplication {
//        mockk<DBOperator> {
//            every { getSessionByID(any()) } returns null
//        }
//
//        val response: HttpResponse = client.get("/api/game/999/mapId")
//
//        assertEquals(HttpStatusCode.BadRequest, response.status)
//
//        assertEquals(
//            "{\"type\":\"error\",\"message\":\"Session #999 does not exist\"}",
//            response.bodyAsText()
//        )
//    }
//
//    @Test
//    fun `GET request to api-pictures returns expected response`() = testApplication {
//        mockk<DBOperator> {
//            every { getAllPictures() } returns listOf(
//                PictureInfo(1u, "./path/to/picture1.png"),
//                PictureInfo(2u, "./path/to/picture2.png")
//            )
//        }
//
//        val response: HttpResponse = client.get("/api/pictures")
//
//        assertEquals(HttpStatusCode.OK, response.status)
//
//        assertEquals(
//            "{\"type\":\"ok\",\"result\":[{\"id\":\"1\",\"filepath\":\"./path/to/picture1.png\"},{\"id\":\"2\",\"filepath\":\"./path/to/picture2.png\"}]}",
//            response.bodyAsText()
//        )
//    }
//
//    @Test
//    fun `GET request to api-pictures-id returns expected response`() = testApplication {
//        mockk<DBOperator> {
//            every { getPictureByID(any()) } returns PictureInfo(1u, ".\\pictures\\picture1.png")
//        }
//
//        val response: HttpResponse = client.get("/api/pictures/1")
//
//        assertEquals(HttpStatusCode.OK, response.status)
//
//        assertEquals(
//            "{\"type\":\"ok\",\"result\":{\"filepath\":\".\\\\pictures\\\\picture1.png\",\"id\":\"1\"}}",
//            response.bodyAsText()
//        )
//    }
//
//    @Test
//    fun `GET request to non-existing api-pictures-id returns 400 error`() = testApplication {
//        mockk<DBOperator> {
//            every { getPictureByID(any()) } returns null
//        }
//
//        val response: HttpResponse = client.get("/api/pictures/999")
//
//        assertEquals(HttpStatusCode.BadRequest, response.status)
//
//        assertEquals(
//            "{\"type\":\"error\",\"message\":\"Picture #999 does not exist\"}",
//            response.bodyAsText()
//        )
//    }
//
//    @Test
//    fun `POST request to api-pictures returns expected response`() = testApplication {
//        mockk<DBOperator> {
//            every { addPicture(any()) } returns PictureInfo(1u, ".\\pictures\\picture1.png")
//        }
//
//        val response: HttpResponse = client.post("/api/pictures") {
//            setBody("test image content")
//        }
//
//        assertEquals(HttpStatusCode.OK, response.status)
//
//        assertEquals(
//            "{\"type\":\"ok\",\"result\":{\"id\":\"1\",\"filepath\":\".\\\\pictures\\\\picture1.png\"}}",
//            response.bodyAsText()
//        )
//    }
//
//    @Test
//    fun `GET request to api-user returns expected response`() = testApplication {
//        mockk<DBOperator> {
//            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
//        }
//
//        val loginBody = """{"login": "testLogin", "password": "testPassword"}"""
//
//        val login: HttpResponse = client.post("/api/login") {
//            setBody(loginBody)
//        }
//
//        val token = JSONObject(login.bodyAsText()).get("result").toString()
//
//        val requestBody = """{"id": 1}"""
//
//        val response: HttpResponse = client.post("/api/user") {
//            headers["Authorization"] = "Bearer $token"
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
//
//    @Test
//    fun `GET request to api-user returns error`() = testApplication {
//        mockk<DBOperator> {
//        }
//
//        val requestBody = """{"id": 1}"""
//
//        val response: HttpResponse = client.post("/api/user") {
//            setBody(requestBody)
//        }
//
//        assertEquals(HttpStatusCode.Unauthorized, response.status)
//    }
}
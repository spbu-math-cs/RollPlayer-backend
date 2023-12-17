import db.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import netscape.javascript.JSObject
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import server.module
import java.io.File
import kotlin.test.assertEquals

private fun createErrorResponseMessage(msg: String?) = mapOf(
    "type" to "error", "message" to msg
).toString()


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpsTest {
    @Test
    fun `GET request to api-textures returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllTextures() } returns listOf(
                TextureInfo(1.toUInt(), "/path/to/file1"),
            )
        }

        val response: HttpResponse = client.get("/api/textures")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"result\":[{\"filepath\":\"/path/to/file1\",\"id\":\"1\"}],\"type\":\"ok\"}",
            response.bodyAsText()
        )
    }


    @Test
    fun `GET request to api-textures-id returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllTextures() } returns listOf(
                TextureInfo(1.toUInt(), "/path/to/file1"),
            )
        }

        val response: HttpResponse = client.get("/api/textures/1")

        assertEquals(HttpStatusCode.OK, response.status)

    }


    @Test
    fun `GET request with non-cast-to-UInt to api-textures-id returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllTextures() } returns listOf(
                TextureInfo(1.toUInt(), "/path/to/file1"),
            )
        }

        val response: HttpResponse = client.get("/api/textures/1ewd")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals("{\"type\":\"error\",\"message\":\"Texture #0 does not exist\"}", response.bodyAsText())
    }


    @Test
    fun `GET request to non-existing api-textures-id returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getTextureByID(any()) } returns null
        }

        val response: HttpResponse = client.get("/api/textures/999")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals("{\"type\":\"error\",\"message\":\"Texture #999 does not exist\"}", response.bodyAsText())
    }


    @Test
    fun `GET request to api-tilesets returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllTilesets() } returns listOf(TilesetInfo(1u, "/path/to/tileset1.json"))
        }

        val response: HttpResponse = client.get("/api/tilesets")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"result\":[{\"filepath\":\"./path/to/tileset1.json\",\"id\":\"1\"}],\"type\":\"ok\"}",
            response.bodyAsText()
        )
    }


    @Test
    fun `GET request to api-tilesets-id returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getTilesetByID(any()) } returns TilesetInfo(1u, "/path/to/tileset1.json")
        }

        val response: HttpResponse = client.get("/api/tilesets/1")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{ \"columns\":49, \"image\":\"tileset_packed.png\", \"imageheight\":352, \"imagewidth\":784, \"margin\":0, \"name\":\"tileset_packed\", \"spacing\":0, \"tilecount\":1078, \"tiledversion\":\"1.10.2\", \"tileheight\":16, \"tilewidth\":16, \"type\":\"tileset\", \"version\":\"1.10\" }",
            response.bodyAsText()
        )
    }


    @Test
    fun `GET request to non-existing api-tilesets returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getAllTilesets() } returns emptyList()
        }

        val response: HttpResponse = client.get("/api/tilesets/100")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"Tileset #100 does not exist\"}",
            response.bodyAsText()
        )
    }


    @Test
    fun `GET request to api-maps returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllMaps() } returns listOf(MapInfo(1u, "/path/to/map1.json"))
        }

        val response: HttpResponse = client.get("/api/maps")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"result\":[{\"filepath\":\"./path/to/map1.json\",\"id\":\"1\"}],\"type\":\"ok\"}",
            response.bodyAsText()
        )
    }


    @Test
    fun `GET request to api-maps-id returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getMapByID(any()) } returns MapInfo(1u, "/path/to/map1.json")
        }

        val response: HttpResponse = client.get("/api/maps/1")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(File("./resources/maps/example_map.tmj").readText(), response.bodyAsText())
    }


    @Test
    fun `GET request to api-maps returns expected response returns error`() = testApplication {
        mockk<DBOperator> {
            every { getMapByID(any()) } returns null
        }

        val response: HttpResponse = client.get("/api/maps/100")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"Map #100 does not exist\"}",
            response.bodyAsText()
        )

    }


    @Test
    fun `GET request to api-users returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllUsers() } returns listOf(
                UserInfo(1u, "testLogin", "test@email.ru", 1930943205, null),
                UserInfo(2u, "testLogin2", "test2@email.ru", 1930943205, null)
            )
        }

        val response: HttpResponse = client.get("/api/users")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"result\":[{\"avatarID\":null,\"id\":1,\"login\":\"testLogin\",\"email\":\"test@email.ru\",\"passwordHash\":1930943205},{\"avatarID\":null,\"id\":2,\"login\":\"testLogin2\",\"email\":\"test2@email.ru\",\"passwordHash\":1240535341}],\"type\":\"ok\"}",
            response.bodyAsText()
        )
    }


    @Test
    fun `GET request to non-existing api-users returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllUsers() } returns emptyList()
        }

        val response: HttpResponse = client.get("/api/users")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals("{\"result\":[],\"type\":\"ok\"}", response.bodyAsText())
    }


    @Test
    fun `POST request to api-register returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { addUser(any(), any(), any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getUserByID(1u) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
        }

        val requestBody = """{"login": "testLogin", "email": "test@email.ru", "password": "testPassword"}"""

        val response: HttpResponse = client.post("/api/register") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Created, response.status)

        assertEquals(
            "{\"result\":{\"avatarID\":null,\"id\":1,\"login\":\"testLogin\",\"email\":\"test@email.ru\",\"passwordHash\":1484766988},\"type\":\"ok\",\"message\":\"User 1 registered successfully\"}",
            response.bodyAsText()
        )
    }

    @Test
    fun `POST request with incorrect password to api-register returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
        }

        val requestBody = """{"login": "testLogin", "email": "incorrectEmail", "password": "pass"}"""

        val response = client.post("/api/register") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"User password must have at least 8 characters\"}",
            response.bodyAsText()
        )
    }

    @Test
    fun `POST request with existing email to api-register returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
        }

        val requestBody = """{"login": "testLogin", "email": "test@email.ru", "password": "testPassword"}"""

        client.post("/api/register") { setBody(requestBody) }

        val response = client.post("/api/register") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"User with login `testLogin` already exists\"}",
            response.bodyAsText()
        )
    }

    @Test
    fun `POST request with incorrect email to api-register returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
        }

        val requestBody = """{"login": "testLogin", "email": "incorrectEmail", "password": "testPassword"}"""

        val response = client.post("/api/register") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"Email does not match regex ^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z.-]+\$\"}",
            response.bodyAsText()
        )
    }

    @Test
    fun `POST request to api-login returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getUserByLogin(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { checkUserPassword(userId = 1u, any()) } returns true
        }

        val requestBody = """{"login": "testLogin", "password": "testPassword"}"""

        val response: HttpResponse = client.post("/api/login") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals("User 1 logged in successfully", JSONObject(response.bodyAsText()).getString("message"))
    }


    @Test
    fun `POST request to non-existing api-login returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getUserByLogin(any()) } returns null
        }

        val requestBody = """{"login": "nonExistingLogin", "password": "testPassword"}"""

        val response: HttpResponse = client.post("/api/login") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"User with this login/email is not exist\"}",
            response.bodyAsText()
        )
    }


    @Test
    fun `POST request to api-logout returns expected response`() = testApplication {

        val loginBody = """{"login": "testLogin", "password": "testPassword"}"""

        val login: HttpResponse = client.post("/api/login") {
            setBody(loginBody)
        }

        val token = JSONObject(login.bodyAsText()).get("result").toString()

        val requestBody = "{\"id\": 1}"

        val logout: HttpResponse = client.post("/api/logout") {
            headers["Authorization"] = token
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, logout.status)

    }


    @Test
    fun `POST request to api-logout returns error`() = testApplication {
        mockk<DBOperator> {}

        val requestBody = """{"userId": 1}"""

        val response: HttpResponse = client.post("/api/logout") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)

        assertEquals("Token is not valid or has expired", response.bodyAsText())
    }


    @Test
    fun `POST request to api-edit-userId returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { updateUserLogin(any(), any()) } returns Unit
            every { updateUserEmail(any(), any()) } returns Unit
            every { updateUserPassword(any(), any()) } returns Unit
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
        }

        val loginBody = """{"login": "testLogin", "password": "testPassword"}"""

        val login: HttpResponse = client.post("/api/login") {
            setBody(loginBody)
        }

        val token = JSONObject(login.bodyAsText()).get("result").toString()

        val requestBody = """{"login": "newLogin", "email": "test1@email.ru", "password": "testPassword"}"""
        val response: HttpResponse = client.post("/api/user/edit") {
            headers["Authorization"] = token
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"result\":{\"avatarID\":null,\"id\":1,\"login\":\"newLogin\",\"email\":\"test1@email.ru\",\"passwordHash\":796678170},\"type\":\"ok\",\"message\":\"Data for user 1 edit successfully\"}",
            response.bodyAsText()
        )
    }

    @Test
    fun `POST request to api-edit-userId without JWT returns error`() = testApplication {
        mockk<DBOperator> {
            every { updateUserLogin(any(), any()) } returns Unit
            every { updateUserEmail(any(), any()) } returns Unit
            every { updateUserPassword(any(), any()) } returns Unit
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
        }

        val requestBody = """{"login": "newLogin", "email": "test1@email.ru", "password": "testPassword"}"""

        val response: HttpResponse = client.post("/api/user/edit") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "Token is not valid or has expired",
            response.bodyAsText()
        )
    }

    @Test
    fun `GET request to api-userId-sessions returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllSessionsWithUser(any()) } returns emptyList()
        }

        val requestBody = """{"id": 1}"""

        val response = client.get("/api/user/sessions"){
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals("{\"result\":[],\"type\":\"ok\"}",
            response.bodyAsText())
    }

    @Test
    fun `GET request to non-existing api-userId-sessions returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getAllSessionsWithUser(any()) } throws Throwable("User #100 does not exist")
        }

        val response = client.get("/api/100/sessions")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }


    @Test
    fun `GET request with non-cast-to-UInt id to api-userId-sessions returns 400 error`() = testApplication {
        mockk<DBOperator> {}

        val response = client.get("/api/nonUIntId/sessions")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST request to api-game-create returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { addSession(any()) } returns SessionInfo(1u, 2u, true, Clock.System.now(), 0)
        }

        val response: HttpResponse = client.post("/api/game/create?mapId=2")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"type\":\"ok\",\"message\":\"Session created\",\"result\":{\"sessionId\":1,\"mapId\":2,\"active\":true,\"started\":\"${Clock.System.now()}\",\"prevCharacterId\":0}}",
            response.bodyAsText()
        )
    }

    @Test
    fun `POST request to api-game-create without mapId returns 400 error`() = testApplication {
        mockk<DBOperator> {}

        val response: HttpResponse = client.post("/api/game/create")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"Request must contain \\\"mapId\\\" query parameter\"}",
            response.bodyAsText()
        )
    }

    @Test
    fun `GET request to api-game-sessionId-mapId returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getSessionByID(any()) } returns SessionInfo(1u, 2u, true, Clock.System.now(), 0)
        }

        val response: HttpResponse = client.get("/api/game/1/mapId")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"type\":\"ok\",\"result\":{\"id\":1,\"mapID\":2,\"active\":true,\"started\":\"${Clock.System.now()}\",\"prevCharacterId\":0}}",
            response.bodyAsText()
        )
    }

    @Test
    fun `GET request to non-existing api-game-sessionId-mapId returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getSessionByID(any()) } returns null
        }

        val response: HttpResponse = client.get("/api/game/999/mapId")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"Session #999 does not exist\"}",
            response.bodyAsText()
        )
    }

    @Test
    fun `GET request to api-pictures returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllPictures() } returns listOf(
                PictureInfo(1u, "./path/to/picture1.png"),
                PictureInfo(2u, "./path/to/picture2.png")
            )
        }

        val response: HttpResponse = client.get("/api/pictures")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"type\":\"ok\",\"result\":[{\"id\":\"1\",\"filepath\":\"./path/to/picture1.png\"},{\"id\":\"2\",\"filepath\":\"./path/to/picture2.png\"}]}",
            response.bodyAsText()
        )
    }

    @Test
    fun `GET request to api-pictures-id returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getPictureByID(any()) } returns PictureInfo(1u, ".\\pictures\\picture1.png")
        }

        val response: HttpResponse = client.get("/api/pictures/1")

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"type\":\"ok\",\"result\":{\"filepath\":\".\\\\pictures\\\\picture1.png\",\"id\":\"1\"}}",
            response.bodyAsText()
        )
    }

    @Test
    fun `GET request to non-existing api-pictures-id returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getPictureByID(any()) } returns null
        }

        val response: HttpResponse = client.get("/api/pictures/999")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals(
            "{\"type\":\"error\",\"message\":\"Picture #999 does not exist\"}",
            response.bodyAsText()
        )
    }

    @Test
    fun `POST request to api-pictures returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { addPicture(any()) } returns PictureInfo(1u, ".\\pictures\\picture1.png")
        }

        val response: HttpResponse = client.post("/api/pictures") {
            setBody("test image content")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(
            "{\"type\":\"ok\",\"result\":{\"id\":\"1\",\"filepath\":\".\\\\pictures\\\\picture1.png\"}}",
            response.bodyAsText()
        )
    }
}
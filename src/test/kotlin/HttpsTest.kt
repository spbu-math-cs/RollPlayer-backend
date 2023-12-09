import db.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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

        val response: HttpResponse = client.get("/api/textures/1")

        assertEquals(HttpStatusCode.OK, response.status)
    }


    @Test
    fun `GET request to non-existing api-textures-id returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getTextureByID(any()) } returns null
        }

        val response: HttpResponse = client.get("/api/textures/999")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }


    @Test
    fun `GET request to api-tilesets returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllTilesets() } returns listOf(TilesetInfo(1u, "/path/to/tileset1.json"))
        }

        val response: HttpResponse = client.get("/api/tilesets")

        assertEquals(HttpStatusCode.OK, response.status)
    }


    @Test
    fun `GET request to api-tilesets-id returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getTilesetByID(any()) } returns TilesetInfo(1u, "/path/to/tileset1.json")
        }

        val response: HttpResponse = client.get("/api/tilesets/1")

        assertEquals(HttpStatusCode.OK, response.status)
    }


    @Test
    fun `GET request to non-existing api-tilesets returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getAllTilesets() } returns emptyList()
        }

        val response: HttpResponse = client.get("/api/tilesets/100")

        assertEquals(HttpStatusCode.OK, response.status)
    }


    @Test
    fun `GET request to api-maps returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllMaps() } returns listOf(MapInfo(1u, "/path/to/map1.json"))
        }

        val response: HttpResponse = client.get("/api/maps")

        assertEquals(HttpStatusCode.OK, response.status)
    }


    @Test
    fun `GET request to api-maps-id returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getMapByID(any()) } returns MapInfo(1u, "/path/to/map1.json")
        }

        val response: HttpResponse = client.get("/api/maps/1")

        assertEquals(HttpStatusCode.OK, response.status)
    }


    @Test
    fun `GET request to api-maps returns expected response returns error`() = testApplication {
        mockk<DBOperator> {
            every { getMapByID(any()) } returns null
        }

        val response: HttpResponse = client.get("/api/maps/100")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }


    @Test
    fun `GET request to api-users returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllUsers() } returns listOf(UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u))
        }

        val response: HttpResponse = client.get("/api/users")

        assertEquals(HttpStatusCode.OK, response.status)

    }


    @Test
    fun `GET request to non-existing api-users returns 400 error`() = testApplication {
        mockk<DBOperator> {
            every { getAllUsers() } returns emptyList()
        }

        val response: HttpResponse = client.get("/api/users")

        assertEquals(HttpStatusCode.BadRequest, response.status)
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
    }


    @Test
    fun `POST request to api-logout returns expected response`() = testApplication {
        mockk<DBOperator> {}

        val requestBody = """{"userId": 1}"""

        val response: HttpResponse = client.post("/api/logout") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }


    @Test
    fun `POST request to api-edit-userId returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { updateUserLogin(any(), any()) } returns Unit
            every { updateUserEmail(any(), any()) } returns Unit
            every { updateUserPassword(any(), any()) } returns Unit
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
        }

        val requestBody = """{"login": "newLogin", "email": "newEmail", "password": "newPassword"}"""
        val response: HttpResponse = client.post("/api/edit/1") {
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)
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
    }


    @Test
    fun `GET request to api-userId-sessions returns expected response`() = testApplication {
        mockk<DBOperator> {
            every { getAllSessionsWithUser(any()) } returns emptyList()
        }

        val response = client.get("/api/1/sessions")

        assertEquals(HttpStatusCode.OK, response.status)
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
}
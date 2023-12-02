import db.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.*
import server.ActiveSessionData
import server.Connection
import server.module
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class HttpsTest {

    @MockK(relaxed = true)
    lateinit var connection1: Connection

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(DBOperator)
        every { DBOperator.createDBForTests(any()) } just runs
    }

    @AfterAll
    fun cleanup() {
        unmockkAll()
    }


    @Test
    fun `startConnection should add characters to connection`() = runBlocking {
        every { DBOperator.getCharacterByID(any()) } returns
                CharacterInfo(1u, 1u, 1u, "123", 1, 1)
        every { DBOperator.getAllCharactersOfUserInSession(any(), any()) } returns setOf(
            CharacterInfo(1u, 1u, 1u, "1234", 1, 1),
            CharacterInfo(2u, 1u, 1u, "12345", 2, 2)
        ).toList()

        val sessionId = 1u
        val mapId = 1u
        val session = ActiveSessionData(sessionId, mapId, Clock.System.now())
        val connection = mockk<Connection>()

        launch {
            //ActiveSessionData().startConnection(1u, connection, "TestAddress")
        }
        delay(100)
        coVerify(exactly = 2) { connection.connection.send(any()) }
    }


    @Test
    fun `test deleteTextureByID`() {
        val textureId = 1u
        every { DBOperator.deleteTextureByID(any()) } returns true
        val result = DBOperator.deleteTextureByID(textureId)
        verify { DBOperator.deleteTextureByID(textureId) }
        Assertions.assertTrue(result)
    }

    @Test
    fun `test deleteUserByID 1`() {
        val userId = 1u
        every { DBOperator.deleteUserByID(any()) } returns true
        val result = DBOperator.deleteUserByID(userId)
        verify { DBOperator.deleteUserByID(userId) }
        Assertions.assertTrue(result)
    }


    @Test
    fun `finishConnection should remove characters from session`() = runBlocking{
        every { DBOperator.getUserByID(any()) } returns mockk {
            every { id } returns 1u
        }
        every { DBOperator.getAllCharactersOfUserInSession(any(), any()) } returns
                listOf(
                    CharacterInfo(1u, 1u, 1u, "1234", 1, 1),
                    CharacterInfo(2u, 1u, 1u, "12345", 2, 2)
                )


        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = Instant.DISTANT_PAST
        )
        activeSessionData.startConnection(1u, connection1, "TestAddress")
        activeSessionData.finishConnection(1u, connection1, "TestAddress")

        assertTrue(activeSessionData.connections.isEmpty())
    }


    @Test
    fun `GET request to api-textures returns expected response`() {
        withTestApplication({
            mockk<DBOperator> {
                every { getAllTextures() } returns listOf(
                    TextureInfo(1.toUInt(), "/path/to/file1"),
                )
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/textures")
            Assertions.assertEquals(HttpStatusCode.OK, response.response.status())
        }
    }


    @Test
    fun `GET request to api-textures-id returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getTextureByID(any()) } returns TextureInfo(1u, "/path/to/file1")
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/textures/1")
            Assertions.assertEquals(HttpStatusCode.OK, response.response.status())
        }
    }

    @Test
    fun `GET request to non-existing api-textures-id returns 404 error`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getTextureByID(any()) } returns null
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/textures/999")
            Assertions.assertEquals(HttpStatusCode.NotFound, response.response.status())
        }
    }


    @Test
    fun `GET request to api-tilesets returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getAllTilesets() } returns listOf(TilesetInfo(1u, "/path/to/tileset1.json"))
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/tilesets")
            Assertions.assertEquals(HttpStatusCode.OK, response.response.status())

        }
    }

    @Test
    fun `GET request to api-tilesets-id returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getTilesetByID(any()) } returns TilesetInfo(1u, "/path/to/tileset1.json")
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/tilesets/1")
            Assertions.assertEquals(HttpStatusCode.OK, response.response.status())
        }
    }

    @Test
    fun `GET request to non-existing api-tilesets returns 404 error`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getAllTilesets() } returns emptyList()
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/tilesets")
            Assertions.assertEquals(HttpStatusCode.NotFound, response.response.status())
        }
    }


    @Test
    fun `GET request to api-maps returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getAllMaps() } returns listOf(MapInfo(1u, "/path/to/map1.json"))
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/maps")

            Assertions.assertEquals(HttpStatusCode.OK, response.response.status())
        }
    }

    @Test
    fun `GET request to api-textures return error response`() {
        val exception = java.lang.RuntimeException("Can't connect to database");
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getAllTextures() } throws exception
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/textures")
            assertEquals(HttpStatusCode.BadRequest, response.response.status())
            assertEquals(response.response.content, createErrorResponseMessage(exception.message))
        }
    }

    private fun createErrorResponseMessage(msg: String?) = mapOf(
        "type" to "error",
        "message" to msg
    ).toString()

    @Test
    fun `GET request to api-maps returns expected response returns error`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getMapByID(any()) } returns null
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/maps/2")
            Assertions.assertEquals(HttpStatusCode.NotFound, response.response.status())
        }
    }

    @Test
    fun `GET request to api-maps-id returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getMapByID(any()) } returns MapInfo(1u, "/path/to/map1.json")
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/maps/1")
            Assertions.assertEquals(HttpStatusCode.OK, response.response.status())
        }
    }


    @Test
    fun `GET request to non-existing file path returns error`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getMapByID(any()) } returns null
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/maps/2")
            Assertions.assertEquals(HttpStatusCode.NotFound, response.response.status())
        }
    }


    @Test
    fun `POST request to api-register returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { addUser(any(), any(), any()) } returns 1u
                every { getUserByID(1u) } returns UserInfo(1u, "testLogin", "testEmail", 1234567890)
            }

            module()
        }) {
            val requestBody = """{"login": "testLogin", "email": "testEmail", "password": "testPassword"}"""
            val response = handleRequest(HttpMethod.Post, "/api/register") {
                setBody(requestBody)
            }

            Assertions.assertEquals(HttpStatusCode.Created, response.response.status())

        }
    }

    @Test
    fun `GET request to api-users returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getAllUsers() } returns listOf(UserInfo(1u, "testLogin", "testEmail", 1234567890))
            }

            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/users")

            Assertions.assertEquals(HttpStatusCode.OK, response.response.status())

        }
    }

    @Test
    fun `GET request to non-existing api-users returns 404 error`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getAllUsers() } returns emptyList()
            }
            module()
        }) {
            val response = handleRequest(HttpMethod.Get, "/api/users")
            Assertions.assertEquals(HttpStatusCode.NotFound, response.response.status())
        }
    }


    @Test
    fun `POST request to api-login returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getUserByLogin(any()) } returns UserInfo(1u, "testLogin", "testEmail", 1234567890)
                every { checkUserPassword(userId = 1u, any()) } returns true
            }

            module()
        }) {
            val requestBody = """{"login": "testLogin", "password": "testPassword"}"""
            val response = handleRequest(HttpMethod.Post, "/api/login") {
                setBody(requestBody)
            }

            Assertions.assertEquals(HttpStatusCode.OK, response.response.status())

        }
    }

    @Test
    fun `POST request to non-existing api-login returns 404 error`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { getUserByLogin(any()) } returns null
            }
            module()
        }) {
            val requestBody = """{"login": "nonExistingLogin", "password": "testPassword"}"""
            val response = handleRequest(HttpMethod.Post, "/api/login") {
                setBody(requestBody)
            }

            Assertions.assertEquals(HttpStatusCode.NotFound, response.response.status())
        }
    }


    @Test
    fun `POST request to api-logout returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {}

            module()
        }) {
            val requestBody = """{"userId": 1}"""
            val response = handleRequest(HttpMethod.Post, "/api/logout") {
                setBody(requestBody)
            }

            Assertions.assertEquals(HttpStatusCode.OK, response.response.status())
        }
    }

    @Test
    fun `POST request to api-edit-userId returns expected response`() {
        withTestApplication({
            val mockDBOperator = mockk<DBOperator> {
                every { updateUserLogin(any(), any()) } returns Unit
                every { updateUserEmail(any(), any()) } returns Unit
                every { updateUserPassword(any(), any()) } returns Unit
                every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "testEmail", 1234567890)
            }
            module()
        }) {
            val requestBody = """{"login": "newLogin", "email": "newEmail", "password": "newPassword"}"""
            val response = handleRequest(HttpMethod.Post, "/api/edit/1") {
                setBody(requestBody)
            }
            Assertions.assertEquals(HttpStatusCode.Created, response.response.status())
        }
    }
}
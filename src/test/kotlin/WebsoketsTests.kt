import db.CharacterInfo
import db.DBOperator
import io.ktor.websocket.*
import io.mockk.*
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.*
import server.ActiveSessionData
import server.Connection
import server.routing.finishConnection
import server.routing.startConnection


@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class WebsoketsTests {

    @BeforeEach
    fun setup() {
        mockkObject(DBOperator)
        every { DBOperator.createDBForTests(any()) } just runs
    }

    @AfterAll
    fun cleanup() {
        unmockkAll()
    }


    @Test
    suspend fun `startConnection should add characters to connection`() = runBlocking {
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
            startConnection(session, 1u, connection, "TestAddress")
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
        assertTrue(result)
    }

    @Test
    fun `test deleteUserByID`() {
        val userId = 1u
        every { DBOperator.deleteUserByID(any()) } returns true
        val result = DBOperator.deleteUserByID(userId)
        verify { DBOperator.deleteUserByID(userId) }
        assertTrue(result)
    }

    @Test
    suspend fun `finishConnection should remove characters from session`() {
        every { DBOperator.getUserByID(any()) } returns mockk {
            every { id } returns 1u
        }
        val sessionId = 1u
        val mapId = 1u
        val session = ActiveSessionData(sessionId, mapId, Clock.System.now())
        val connection = mockk<Connection>()
        session.characters.add(1u)
        finishConnection(session, 1u, connection, "TestAddress")
        coVerify { connection.connection.send(any()) }
    }

}

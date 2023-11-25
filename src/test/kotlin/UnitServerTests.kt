import db.CharacterInfo
import db.DBOperator
import db.SessionInfo
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.h2.engine.Database
import org.json.JSONObject
import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import server.ActiveSessionData
import server.Connection
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import java.time.Instant as JavaInstant

class ActiveSessionDataTest {

    @MockK(relaxed = true)
    lateinit var connection1: Connection

    @MockK(relaxed = true)
    lateinit var connection2: Connection

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DBOperator)
        every { DBOperator.createDBForTests(any()) } just runs

    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }


    @Test
    fun `test updateMoveProperties`() = runBlocking {
        val distantPastInstant = Instant.fromEpochSeconds(JavaInstant.MIN.epochSecond)

        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = distantPastInstant
        )

        activeSessionData.connections.add(connection1)
        activeSessionData.connections.add(connection2)

        coEvery { connection1.connection.send(any()) } just runs
        coEvery { connection2.connection.send(any()) } just runs

        //activeSessionData.updateCharactersStatus()

        coVerify(exactly = 1) {
            connection1.connection.send(any())
        }
        coVerify(exactly = 1) {
            connection2.connection.send(any())
        }

        //assertEquals(connection2.id, activeSessionData.MoveProperties())
    }


    @Test
    fun `test getJson`() {
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = started
        )

        assertEquals(
            "{\"mapId\":\"1\",\"started\":\"$started\",\"sessionId\":\"1\"}",
            activeSessionData.toJson()
        )
    }

    @Test
    fun `test toSessionInfo`() {
        val sessionId = 1u
        val mapId = 1u
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = sessionId,
            mapId = mapId,
            started = started
        )

        assertEquals(
            SessionInfo(sessionId, mapId, active = true, started, whoCanMove = -1),
            activeSessionData.toSessionInfo()
        )
    }

    @Test
    fun `test validateMoveAndUpdateMoveProperties incorrect characterId`() {

        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = Instant.DISTANT_PAST
        )

        assertFails { activeSessionData.validateMoveAndUpdateMoveProperties(0u) }
    }

    @Test
    fun `test addCharacterToSession`() = runBlocking {

        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = Instant.DISTANT_PAST
        )

        val characterInfo = CharacterInfo(
            id = 0u,
            userId = 1u,
            sessionId = 1u,
            name = "character1",
            row = 0,
            col = 0
        )

        activeSessionData.addCharacterToSession(characterInfo, connection1)

        assertEquals(connection1, activeSessionData.charactersToConnection[0u])
    }

    @Test
    fun `test validateMoveAndUpdateMoveProperties correct characterId`() = runBlocking {

        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = Instant.DISTANT_PAST
        )

        val characterInfo = CharacterInfo(
            id = 0u,
            userId = 1u,
            sessionId = 1u,
            name = "character1",
            row = 0,
            col = 0
        )

        activeSessionData.addCharacterToSession(characterInfo, connection1)

        activeSessionData.validateMoveAndUpdateMoveProperties(0u)

        assertEquals(AtomicInteger(0).get(), activeSessionData.moveProperties.prevCharacterMovedId.get())

    }

    @Test
    fun `test getValidCharacter correct characterId`(): Unit = runBlocking {
        every { DBOperator.getCharacterByID(any()) } returns
                CharacterInfo(1u, 1u, 1u, "123", 1, 1)

        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = Instant.DISTANT_PAST
        )

        val characterInfo = CharacterInfo(
            id = 0u,
            userId = 1u,
            sessionId = 1u,
            name = "character1",
            row = 0,
            col = 0
        )

        every { DBOperator.getCharacterByID(any()) } returns
                CharacterInfo(0u, 1u, 1u, "123", 1, 1)

        activeSessionData.addCharacterToSession(characterInfo, connection1)

        activeSessionData.validateMoveAndUpdateMoveProperties(0u)

        assertEquals(CharacterInfo(0u, 1u, 1u, "123", 1, 1),
            activeSessionData.getValidCharacter(JSONObject("{\"id\": 0}"), 1u) )
    }
    @Test
    fun `test getValidCharacter incorrect characterId`(): Unit = runBlocking {
        every { DBOperator.getCharacterByID(any()) } returns
                CharacterInfo(1u, 1u, 1u, "123", 1, 1)

        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = Instant.DISTANT_PAST
        )

        val characterInfo = CharacterInfo(
            id = 0u,
            userId = 1u,
            sessionId = 1u,
            name = "character1",
            row = 0,
            col = 0
        )

        activeSessionData.addCharacterToSession(characterInfo, connection1)

        activeSessionData.validateMoveAndUpdateMoveProperties(0u)

        assertFails{ activeSessionData.getValidCharacter(JSONObject("{\"id\": 0}"), 0u) }
    }

    @Test
    fun `test startConnection`(): Unit = runBlocking {

        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = Instant.DISTANT_PAST
        )

        every { DBOperator.getCharacterByID(any()) } returns
                CharacterInfo(1u, 1u, 1u, "123", 1, 1)




        activeSessionData.startConnection(1u, connection1, "127.0.0.1")

    }
}
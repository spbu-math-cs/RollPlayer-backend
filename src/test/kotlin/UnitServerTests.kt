import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import server.ActiveSessionData
import server.Connection
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Instant as JavaInstant

class ActiveSessionDataTest {

    @MockK(relaxed = true)
    lateinit var connection1: Connection

    @MockK(relaxed = true)
    lateinit var connection2: Connection

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
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

//
//
//    @Test
//    fun `test validateMove success`() {
//        val activeSessionData = ActiveSessionData(
//            sessionId = 1u,
//            mapId = 1u,
//            started = Instant.DISTANT_PAST
//        )
//        activeSessionData.moveProperties.whoCanMove.set(1)
//
//        activeSessionData.validateMove(1)
//    }
//
//    @Test
//    fun `test validateMove failure`() {
//        val activeSessionData = ActiveSessionData(
//            sessionId = 1u,
//            mapId = 1u,
//            started = Instant.DISTANT_PAST
//        )
//        activeSessionData.moveProperties.whoCanMove.set(1)
//
//        assertFailsWith<Exception> {
//            activeSessionData.validateMove(2)
//        }
//    }
}
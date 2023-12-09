import db.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Database
import org.json.JSONObject
import org.junit.jupiter.api.*
import server.ActiveSessionData
import server.Connection
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlin.test.expect
import java.time.Instant as JavaInstant


private const val TEST_FOLDER = "for_tests"
private val sampleMapFiles = listOf(
    "sample_map_for_test_A",
    "sample_map_for_test_B"
)
class ActiveSessionDataTest {

    @MockK(relaxed = true)
    lateinit var connection1: Connection

    @MockK(relaxed = true)
    lateinit var connection2: Connection

    //    CharacterInfo(
//    1u,
//    1u,
//    1u,
//    "123",
//    null,
//    1,
//    1,
//    BasicProperties(),
//    mapOf("prop1" to 1, "prop2" to 2, "prop3" to 3)
//    )
    @Test
    fun `startConnection should add user to connection`() = runBlocking {
        val distantPastInstant = Instant.fromEpochSeconds(JavaInstant.MIN.epochSecond)

        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = distantPastInstant
        )

        activeSessionData.startConnection(0u, connection1)
        assertEquals(
            expected = "{0=UserData(userId=0, sessionId=1, connections=[Connection(connection1#4)], characters=[])}",
            actual = activeSessionData.activeUsers.toString()
        )
    }


    @Test
    fun `finishConnection should remove user from connection`() = runBlocking{
        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = Instant.fromEpochSeconds(JavaInstant.MIN.epochSecond)
        )

        activeSessionData.startConnection(0u, connection1)
        activeSessionData.startConnection(1u, connection1)

        activeSessionData.finishConnection(1u, connection1)

        assertEquals(
            expected = "{0=UserData(userId=0, sessionId=1, connections=[Connection(connection1#1)], characters=[])}",
            actual = activeSessionData.activeUsers.toString()
        )
    }

    @Test
    fun `test addCharacter`(): Unit = runBlocking {
        val activeSessionData = ActiveSessionData(
            sessionId = 1u,
            mapId = 1u,
            started = Instant.fromEpochSeconds(JavaInstant.MIN.epochSecond)
        )

        activeSessionData.startConnection(0u, connection1)

        assertFails {
            activeSessionData.addCharacter(
                CharacterInfo(
                    0u,
                    0u,
                    1u,
                    "character1",
                    null,
                    1,
                    1,
                    false,
                    BasicProperties(),
                    mapOf("prop1" to 1, "prop2" to 2, "prop3" to 3)
                )
            )
        }
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
            SessionInfo(sessionId, mapId, active = true, started, -1),
            activeSessionData.toSessionInfo()
        )
    }

//    @Test
//    fun `test validateMoveAndUpdateMoveProperties incorrect characterId`() {
//
//        val activeSessionData = ActiveSessionData(
//            sessionId = 1u,
//            mapId = 1u,
//            started = Instant.DISTANT_PAST
//        )
//
//        assertFails { activeSessionData.validateActionAndUpdateActionProperties(0u) }
//    }

    @Test
    fun `test validateMoveAndUpdateMoveProperties correct characterId`(){

    }


    @BeforeEach
    fun setUp(){
        MockKAnnotations.init(this)
    }
    @AfterEach
    fun clearDatabase() {
        DBOperator.deleteAllSessions()
        DBOperator.getAllMaps().forEach { DBOperator.deleteMapByID(it.id) }
        DBOperator.getAllUsers().forEach { DBOperator.deleteUserByID(it.id) }
        DBOperator.deleteAllTextures()
        unmockkAll()
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun createTestDB() {
            DBOperator.createDBForTests()
        }

        @JvmStatic
        @AfterAll
        fun deleteDB() {
            DBOperator.deleteTestDatabase()
            File("$mapsFolder/$TEST_FOLDER")
                .let { file -> if (file.isDirectory) file.delete() }
            sampleMapFiles
                .forEach {
                    File("$mapsFolder/$it.json").apply { if (isFile) delete() }
                }
            File("$texturesFolder/$TEST_FOLDER")
                .let { file -> if (file.isDirectory) file.delete() }
        }
    }

}
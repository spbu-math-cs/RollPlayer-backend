import db.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.json.JSONObject
import org.junit.jupiter.api.*
import server.ActiveSessionData
import server.Connection
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFails
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

    @Test
    fun `test getValidCharacter with correct parameters`() = runBlocking{
        val sessionId = 1u
        val mapId = 1u
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = sessionId,
            mapId = mapId,
            started = started
        )
        activeSessionData.startConnection(1u, connection1)

        activeSessionData.addCharacter(
            CharacterInfo(
                1u,
                1u,
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

        assertEquals(1u, activeSessionData.getValidCharacter(JSONObject().put("id", 1u), 1u).id)
    }

    @Test
    fun `test getValidCharacter with incorrect userId`(): Unit = runBlocking{
        val sessionId = 1u
        val mapId = 1u
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = sessionId,
            mapId = mapId,
            started = started
        )
        activeSessionData.startConnection(1u, connection1)

        activeSessionData.addCharacter(
            CharacterInfo(
                1u,
                1u,
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

        assertFails { activeSessionData.getValidCharacter(JSONObject().put("id", 1u), 2u) }
    }


    @Test
    fun `test getValidCharacter with incorrect characterId`(): Unit = runBlocking{
        val sessionId = 1u
        val mapId = 1u
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = sessionId,
            mapId = mapId,
            started = started
        )
        activeSessionData.startConnection(1u, connection1)

        activeSessionData.addCharacter(
            CharacterInfo(
                1u,
                1u,
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

        assertFails { activeSessionData.getValidCharacter(JSONObject().put("id", 2u), 1u) }
    }

    @Test
    fun `test getValidOpponentCharacter with correct opponentId`(){
        val sessionId = 1u
        val mapId = 1u
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = sessionId,
            mapId = mapId,
            started = started
        )

        assertEquals(1u, activeSessionData.getValidOpponentCharacter(JSONObject().put("opponentId", 1u)).id)
    }


    @Test
    fun `test getValidOpponentCharacter with incorrect opponentId`(){
        val sessionId = 1u
        val mapId = 1u
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = sessionId,
            mapId = mapId,
            started = started
        )

        assertFails {activeSessionData.getValidOpponentCharacter(JSONObject().put("opponentId", 10u))}
    }


    @Test
    fun `test getValidOpponentCharacter with other sessionId`(){
        val sessionId = 1u
        val mapId = 1u
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = sessionId,
            mapId = mapId,
            started = started
        )

        assertFails {activeSessionData.getValidOpponentCharacter(JSONObject().put("opponentId", 2u))}
    }


    @Test
    fun `test removeCharacter`() = runBlocking {
        val sessionId = 1u
        val mapId = 1u
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = sessionId,
            mapId = mapId,
            started = started
        )

        activeSessionData.startConnection(1u, connection1)

        activeSessionData.addCharacter(
            CharacterInfo(
                1u,
                1u,
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

        activeSessionData.removeCharacter(
            CharacterInfo(
                1u,
                1u,
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

        assertEquals(0, activeSessionData.activeUsers[1u]!!.characters.size)

    }


    @Test
    fun `test moveCharacter`() = runBlocking {
        val sessionId = 1u
        val mapId = 1u
        val started = Instant.DISTANT_PAST

        val activeSessionData = ActiveSessionData(
            sessionId = sessionId,
            mapId = mapId,
            started = started
        )

        activeSessionData.startConnection(1u, connection1)

        activeSessionData.moveCharacter(
            CharacterInfo(
                1u,
                1u,
                1u,
                "character1",
                null,
                1,
                1,
                false,
                BasicProperties(),
                mapOf("CURR_HP" to 1, "MAX_HP" to 2, "prop3" to 3)
            )
        )

        assertEquals(AtomicInteger(1).get(), activeSessionData.actionProperties.prevCharacterId.get())
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
            DBOperator.addSession(1u, true, java.time.Instant.now())
            DBOperator.addSession(2u, true, java.time.Instant.now())
            DBOperator.addUser("Vasia", "vasia@mail.ru", "vasia12345")
            DBOperator.addUser("Semen", "semen@mail.ru", "semen12345")
            DBOperator.addUser("Sasha", "sasha@mail.ru", "sasha12345")
            DBOperator.addCharacter(1u, 1u, "Dragonosaur", null, 1, 2,
                basicProperties = BasicProperties(1, 2, 3, 4, 5, 6))
            DBOperator.addCharacter(3u, 2u, "Berserk", null, 1, 2,
                basicProperties = BasicProperties(1, 2, 3, 4, 5, 6))
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
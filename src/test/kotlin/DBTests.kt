import db.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.IllegalArgumentException
import kotlin.NoSuchElementException

private const val TEST_FOLDER = "for_tests"
private val sampleMapFiles = listOf(
    "sample_map_for_test_A",
    "sample_map_for_test_B"
)

class DBTests {
    @Test
    fun sampleUserTest() {
        DBOperator.addUser("Vasia", "vasia@mail.ru", "vasia12345")
        DBOperator.addUser("Petya", "petya@yandex.ru","petya09876")
        DBOperator.addUser("Clara", "clara@gmail.com","zmxncbva")
        DBOperator.addUser("Dendy", "dendy100100_0101@404.com","zmxncbva")
        DBOperator.addUser("Arben", "arben@postashqiptare.al","qwertyzx")

        assertThrows<IllegalArgumentException> { DBOperator.addUser("12345", "12345@mail.ru","abc") }
        assertThrows<IllegalArgumentException> { DBOperator.addUser("Clara", "clara2@gmail.com","loginalreadyexists") }
        assertThrows<IllegalArgumentException> { DBOperator.addUser("Kim", "kim@mail.ru","비밀번호에잘못된문자가있습니다") }

        assertThrows<IllegalArgumentException> { DBOperator.addUser("12346", "petya@yandex.ru","emailalreadyexists") }
        assertThrows<IllegalArgumentException> { DBOperator.addUser("Wendy", "h@ck3r@O_o.|&|","emailincorrect") }
        assertThrows<IllegalArgumentException> { DBOperator.addUser("Kim", "kimmail.ru","emailincorrect") }

        val users = DBOperator.getAllUsers()
        assert(users.any { it.login == "Vasia" })
        assert(users.any { it.login == "Clara" })
        assert(users.any { it.login == "Dendy" })

        val userVasia = DBOperator.getUserByLogin("Vasia")
        assertNotNull(userVasia)
        assertEquals(userVasia!!.login, "Vasia")
        assertEquals(userVasia.email, "vasia@mail.ru")
        assertEquals("Vasia", DBOperator.getUserByID(userVasia.id)?.login)
        assertEquals("Vasia", DBOperator.getUserByEmail(userVasia.email)?.login)
        assertNull(DBOperator.getUserByID(DBOperator.getAllUsers().maxOf { it.id } + 1u))

        DBOperator.deleteUserByID(userVasia.id)
        assertNull(DBOperator.getUserByID(userVasia.id))
        assertNull(DBOperator.getUserByLogin("Vasia"))
        assertNull(DBOperator.getUserByEmail("vasia@mail.ru"))

        DBOperator.getAllUsers()
            .forEach { DBOperator.deleteUserByID(it.id) }
        assertNull(DBOperator.getUserByLogin("Clara"))
    }

    @Test
    fun userManipulationTest() {
        assertTrue(DBOperator.checkLoginAvailability("Vasia"))
        assertTrue(DBOperator.checkLoginAvailability("Petya"))

        DBOperator.addUser("Vasia", "vasia@mail.ru", "vasia12345")
        DBOperator.addUser("Petya", "petya@yandex.ru","petya09876")
        DBOperator.addUser("Clara", "clara@gmail.com","zmxncbva")
        DBOperator.addUser("Dendy", "dendy@yahoo.com","zmxncbva")
        DBOperator.addUser("Arben", "arben@postashqiptare.al","qwertyzx")

        assertFalse(DBOperator.checkLoginAvailability("Vasia"))
        assertFalse(DBOperator.checkLoginAvailability("Petya"))
        assertFalse(DBOperator.checkLoginAvailability(""))
        assertTrue(DBOperator.checkLoginAvailability("Kira"))
        assertTrue(DBOperator.checkLoginAvailability("Jumbo"))

        assertFalse(DBOperator.checkEmailAvailability("dendy@yahoo.com"))
        assertFalse(DBOperator.checkEmailAvailability("arben@postashqiptare.al"))
        assertFalse(DBOperator.checkEmailAvailability("napoléon@gmail.com"))
        assertFalse(DBOperator.checkEmailAvailability("petya_yandex.ru"))
        assertFalse(DBOperator.checkEmailAvailability("petya@yandexru"))
        assertFalse(DBOperator.checkEmailAvailability(""))

        assertTrue(DBOperator.checkEmailAvailability("kira@gmail.com"))
        assertTrue(DBOperator.checkEmailAvailability("jumbo@mumbo.jumbo"))
        assertTrue(DBOperator.checkEmailAvailability("dendy@yandex.ru"))

        val userIds = DBOperator.getAllUsers()
            .associateBy({ it.login }) { it.id }

        assertTrue(DBOperator.checkUserPassword(userIds["Vasia"]!!, "vasia12345"))
        assertTrue(DBOperator.checkUserPassword(userIds["Clara"]!!, "zmxncbva"))
        assertTrue(DBOperator.checkUserPassword(userIds["Dendy"]!!, "zmxncbva"))

        assertFalse(DBOperator.checkUserPassword(userIds["Vasia"]!!, "petya09876"))
        assertFalse(DBOperator.checkUserPassword(userIds["Clara"]!!, "zmxncbvz"))
        assertFalse(DBOperator.checkUserPassword(userIds["Arben"]!!, "qwertyz"))

        assertNotNull(DBOperator.getUserByLogin("Vasia"))

        DBOperator.updateUserLogin(userIds["Vasia"]!!, "Basil")

        assertThrows<IllegalArgumentException> { DBOperator.updateUserLogin(
            DBOperator.getAllUsers().maxOf { it.id } + 1u,
            "DoesNotExist"
        ) }

        assertNotNull(DBOperator.getUserByLogin("Basil"))
        assertNull(DBOperator.getUserByLogin("Vasia"))
        assertEquals(userIds["Vasia"]!!, DBOperator.getUserByLogin("Basil")!!.id)
        assertTrue(DBOperator.checkUserPassword(userIds["Vasia"]!!, "vasia12345"))

        assertTrue(DBOperator.checkUserPassword(userIds["Petya"]!!, "petya09876"))

        DBOperator.updateUserPassword(userIds["Petya"]!!, "imthecoolest")

        assertFalse(DBOperator.checkUserPassword(userIds["Petya"]!!, "petya09876"))
        assertTrue(DBOperator.checkUserPassword(userIds["Petya"]!!, "imthecoolest"))

        assertThrows<IllegalArgumentException> { DBOperator.updateUserLogin(userIds["Clara"]!!, "Arben") }
        assertThrows<IllegalArgumentException> { DBOperator.updateUserLogin(userIds["Clara"]!!, "") }

        assertThrows<IllegalArgumentException> { DBOperator.updateUserPassword(userIds["Clara"]!!, "qwe") }
        assertThrows<IllegalArgumentException> { DBOperator.updateUserPassword(userIds["Arben"]!!, "karaktere_të_pavlefshme") }

        assertDoesNotThrow { DBOperator.updateUserPassword(userIds["Dendy"]!!, "vasia12345") }
    }

    @Test
    fun passwordValidationTest() {
        assertTrue(DBOperator.checkPasswordValidity("qlaksocifunre"))
        assertTrue(DBOperator.checkPasswordValidity("imthecoolest"))
        assertTrue(DBOperator.checkPasswordValidity("|-|e's_/\\_h@Ck3R"))
        assertTrue(DBOperator.checkPasswordValidity("%a\$b|c*d?e\"f/g&h~i`j"))

        assertFalse(DBOperator.checkPasswordValidity(""))
        assertFalse(DBOperator.checkPasswordValidity("short"))
        assertFalse(DBOperator.checkPasswordValidity("###"))
        assertFalse(DBOperator.checkPasswordValidity("Jsem_nejlepší"))
        assertFalse(DBOperator.checkPasswordValidity("no spaces in password"))
        assertFalse(DBOperator.checkPasswordValidity("હુંસૌથીશાનદારછું"))

        val hash = DBOperator.hashPassword("imthecoolest", 12345, 67890)
        assertEquals(hash, DBOperator.hashPassword("imthecoolest", 12345, 67890))
        assertNotEquals(hash, DBOperator.hashPassword("imthecoolest", 54321, 67890))
        assertNotEquals(hash, DBOperator.hashPassword("imthecoolest", 12345, 98760))
        assertNotEquals(hash, DBOperator.hashPassword("imthecoolest", 12344, 67890))
        assertNotEquals(hash, DBOperator.hashPassword("imthecoolest", 12345, 67891))
        assertNotEquals(hash, DBOperator.hashPassword("qwertyuiop", 12345, 67890))
        assertNotEquals(hash, DBOperator.hashPassword("હુંસૌથીશાનદારછું", 12345, 67890))
    }

    @Test
    fun sampleMapTest() {
        val fileName = sampleMapFiles[0]
        val filePath = "$mapsFolder/$fileName.json"

        val anotherFileName = UUID.randomUUID().toString()
        val anotherFilePath = "$mapsFolder/$TEST_FOLDER/$anotherFileName.json"

        DBOperator.createNewMap(fileName, "TestMap")
        assertEquals(
            """
                    {
                        "name": "TestMap"
                    }
                """.trimIndent(), File(filePath)
                .readText()
        )

        DBOperator.addMap(anotherFilePath)

        val maps = DBOperator.getAllMaps()
        val existingMap = maps.firstOrNull {
            it.pathToJson == filePath
        } ?: fail()
        val nonExistingMap = maps.firstOrNull {
            it.pathToJson == anotherFilePath
        } ?: fail()

        assertEquals(filePath, DBOperator.getMapByID(existingMap.id)?.pathToJson)
        assertEquals(anotherFilePath, DBOperator.getMapByID(nonExistingMap.id)?.pathToJson)
        DBOperator.deleteMapByID(nonExistingMap.id)
        assertNull(DBOperator.getMapByID(nonExistingMap.id))

        // Также удалит все сессии
        DBOperator.getAllMaps()
            .forEach { DBOperator.deleteMapByID(it.id) }
        assertNull(DBOperator.getMapByID(existingMap.id))
    }

    @Test
    fun sampleTextureTest() {
        val fileName = UUID.randomUUID().toString()
        val filePath = "$texturesFolder/$TEST_FOLDER/$fileName.png"

        DBOperator.addTexture(filePath)

        val textures = DBOperator.getAllTextures()
        assertEquals(1, textures.count())
        assertEquals(filePath, textures[0].pathToFile)
        assertEquals(filePath, DBOperator.getTextureByID(textures[0].id)?.pathToFile)

        DBOperator.deleteTextureByID(textures[0].id)
        assertNull(DBOperator.getTextureByID(textures[0].id))
        DBOperator.deleteAllTextures()
        assert(DBOperator.getAllTextures().isEmpty())
    }

    @Test
    fun sampleSessionTest() {
        val mapFileName = sampleMapFiles[1]

        DBOperator.addUser("Vasia", "vasia@mail.ru", "vasia12345")
        DBOperator.addUser("Petya", "petya@gmail.com","petya09876")
        DBOperator.addUser("Clara", "clara@yandex.ru","zmxncbva")
        DBOperator.addUser("Dendy", "dendy@yahoo.com","zmxncbva")

        val playerIds = DBOperator
            .getAllUsers()
            .associateBy({ it.login }) { it.id }

        DBOperator.createNewMap(mapFileName, "TestMap")
        DBOperator.addMap("$mapsFolder/${UUID.randomUUID()}.json")
        val (mapId1, mapId2) = DBOperator.getAllMaps().map { it.id }

        DBOperator.addSession(mapId1, true, Instant.now())
        DBOperator.addSession(mapId1, true, Instant.EPOCH)
        DBOperator.addSession(mapId2, false, Instant.now())
        val (sId1, sId2, sId3) = DBOperator.getAllSessions().map { it.id }

        assert(DBOperator.getAllSessions().any { it.mapID == mapId2 })
        assert(DBOperator.getActiveSessions().all { it.mapID == mapId1 })

        assertTrue(DBOperator.getSessionByID(sId2)?.active ?: false)
        assertDoesNotThrow { DBOperator.setSessionActive(sId2, false) }
        assertFalse(DBOperator.getSessionByID(sId2)?.active ?: true)
        assertDoesNotThrow { DBOperator.setSessionActive(sId2, true) }
        assertTrue(DBOperator.getSessionByID(sId2)?.active ?: false)

        assertThrows<IllegalArgumentException> {
            DBOperator.setSessionActive(maxOf(sId1, sId2, sId3) + 1u, true)
        }

        DBOperator.addCharacter(playerIds["Vasia"]!!, sId1, "Dragonosaur", 1, 2,
            mapOf("hp" to 100, "speed" to 40, "damage" to 50))
        DBOperator.addCharacter(playerIds["Vasia"]!!, sId1, "Mad Professor", 1, 2)
        DBOperator.addCharacter(playerIds["Vasia"]!!, sId2, "Terminator", 1, 3)
        DBOperator.addCharacter(playerIds["Vasia"]!!, sId2, "Terminator", 1, 3)
        DBOperator.addCharacter(playerIds["Petya"]!!, sId1, "Sensei", 2, 3,
            mapOf("damage" to 50))
        DBOperator.addCharacter(playerIds["Petya"]!!, sId3, "Kongzilla", 3, 4,
            mapOf("hp" to 200))
        DBOperator.addCharacter(playerIds["Petya"]!!, sId3, "Hippoceros", 3, 4)
        DBOperator.addCharacter(playerIds["Vasia"]!!, sId2, "Heffalump", 5, 3)
        DBOperator.addCharacter(playerIds["Vasia"]!!, sId3, "Terminator", 1, 3)
        DBOperator.addCharacter(playerIds["Clara"]!!, sId2, "Dragonosaur",5)
        DBOperator.addCharacter(playerIds["Clara"]!!, sId3, "Jabberwock")

        assert(DBOperator.getAllCharacters()
            .count { it.name == "Dragonosaur" } == 2)
        assert(DBOperator.getAllCharacters()
            .count { it.name == "Terminator" } == 3)
        assert(DBOperator.getAllCharacters()
            .filter { it.name == "Terminator" }
            .all { it.userId == playerIds["Vasia"]!! })

        val petyaCharacters = DBOperator.getAllCharactersOfUser(playerIds["Petya"]!!)
        assertEquals(3, petyaCharacters.count())
        assertEquals(listOf("Hippoceros", "Kongzilla", "Sensei"), petyaCharacters.map { it.name }.sorted())
        assertEquals(Pair(3, 4),
            petyaCharacters.filter { it.name == "Kongzilla" }
                .first()
                .let { Pair(it.row, it.col) })
        petyaCharacters.filter { it.name == "Hippoceros" }
            .first()
            .let { hippo ->
                DBOperator.getCharacterByID(hippo.id)
                .let {
                    assertNotNull(it)
                    assert(it!!.name == "Hippoceros")
                    assert(it.userId == playerIds["Petya"])
                } }

        assertEquals(0, DBOperator.getAllCharactersOfUser(playerIds["Dendy"]!!).count())

        val session3Characters = DBOperator.getAllCharactersInSession(sId3)
        assertEquals(4, session3Characters.count())
        assertEquals(2, session3Characters.filter { it.userId == playerIds["Petya"]!! }.count())
        assertEquals(Pair(0, 0), session3Characters
            .filter { it.name == "Jabberwock" }
            .first()
            .let { Pair(it.row, it.col) })
        assertEquals(listOf("Hippoceros", "Jabberwock", "Kongzilla", "Terminator"),
            session3Characters.map { it.name }.sorted())
        assertEquals(listOf("Jabberwock"),
            session3Characters
                .filter { it.userId == playerIds["Clara"]!! }
                .map { it.name })

        assertEquals(listOf<CharacterInfo>(),
            DBOperator.getAllCharactersOfUserInSession(playerIds["Petya"]!!, sId2))
        assertEquals(listOf("Heffalump", "Terminator", "Terminator"),
            DBOperator.getAllCharactersOfUserInSession(playerIds["Vasia"]!!, sId2)
                .map { it.name }
                .sorted())
        assertEquals(listOf("Hippoceros", "Kongzilla"),
            DBOperator.getAllCharactersOfUserInSession(playerIds["Petya"]!!, sId3)
                .map { it.name }
                .sorted())

        val dragonosaurId: UInt
        assertEquals(Pair(1, 2),
            DBOperator.getAllCharactersOfUserInSession(playerIds["Vasia"]!!, sId1)
                .first { it.name == "Dragonosaur" }
                .also { dragonosaurId = it.id }
                .let { Pair(it.row, it.col) })

        DBOperator.moveCharacter(dragonosaurId, 3, 5)
        assertEquals(Pair(3, 5),
            DBOperator.getAllCharactersOfUserInSession(playerIds["Vasia"]!!, sId1)
                .first { it.name == "Dragonosaur" }
                .let { Pair(it.row, it.col) })

        assertEquals(100, DBOperator.getPropertyOfCharacter(dragonosaurId, "hp"))
        assertEquals(40, DBOperator.getPropertyOfCharacter(dragonosaurId, "speed"))
        assertNull(DBOperator.getPropertyOfCharacter(dragonosaurId, "power"))

        DBOperator.setCharacterProperty(dragonosaurId, "hp", 60)
        assertEquals(60, DBOperator.getPropertyOfCharacter(dragonosaurId, "hp"))

        assertEquals(mapOf("hp" to 60, "speed" to 40, "damage" to 50),
            DBOperator.getPropertiesOfCharacter(dragonosaurId))

        DBOperator.setCharacterProperty(dragonosaurId, "power", 30000)
        assertEquals(30000, DBOperator.getPropertyOfCharacter(dragonosaurId, "power"))

        assertEquals(mapOf("hp" to 60, "speed" to 40, "damage" to 50, "power" to 30000),
            DBOperator.getPropertiesOfCharacter(dragonosaurId))

        DBOperator.deleteCharacterById(dragonosaurId)
        assertEquals(listOf("Mad Professor"),
            DBOperator.getAllCharactersOfUserInSession(playerIds["Vasia"]!!, sId1)
                .map { it.name })
        assertNull(DBOperator.getCharacterByID(dragonosaurId))

        DBOperator.addCharacter(playerIds["Dendy"]!!, sId1,
            "Bandersnatch", 6, 7)
        DBOperator.addCharacter(playerIds["Dendy"]!!, sId3,
            "Kraken", 8)
        DBOperator.deleteUserByID(playerIds["Petya"]!!) // all Petya characters removed from sessions

        val fantomasId = DBOperator.addCharacter(playerIds["Dendy"]!!, sId2,
                "Fantômas", -1, -3).id

        assertEquals(listOf("Bandersnatch", "Mad Professor"),
            DBOperator.getAllCharactersInSession(sId1)
                .map { it.name }
                .sorted())
        assertEquals(listOf("Bandersnatch", "Fantômas", "Kraken"),
            DBOperator.getAllCharactersOfUser(playerIds["Dendy"]!!)
                .map { it.name }
                .sorted())
        assertEquals(listOf("Fantômas"),
            DBOperator.getAllCharactersOfUserInSession(playerIds["Dendy"]!!, sId2)
                .map { it.name })
        assertEquals(Pair(8, 0),
            DBOperator.getAllCharacters()
                .first { it.name == "Kraken" }
                .let { Pair(it.row, it.col) })

        assertEquals(mapOf<String, Int>(), DBOperator.getPropertiesOfCharacter(fantomasId))
        DBOperator.setCharacterProperty(fantomasId, "hp", 300)
        DBOperator.setCharacterProperty(fantomasId, "wickedness", 600)
        assertEquals(mapOf("hp" to 300, "wickedness" to 600), DBOperator.getPropertiesOfCharacter(fantomasId))
        DBOperator.updateCharacterProperties(fantomasId, mapOf("hp" to 400, "range" to 500, "damage" to 3000000))
        assertEquals(mapOf("hp" to 400, "wickedness" to 600, "range" to 500, "damage" to 3000000),
            DBOperator.getPropertiesOfCharacter(fantomasId))

        DBOperator.deleteSessionByID(sId1)
        assertNull(DBOperator.getSessionByID(sId1))
        assertThrows<IllegalArgumentException> { DBOperator.getAllUsersInSession(sId1) }
        assertThrows<NoSuchElementException> { DBOperator.getAllCharactersInSession(sId1).first() }
        assertEquals(listOf("Fantômas", "Kraken"),
            DBOperator.getAllCharactersOfUser(playerIds["Dendy"]!!)
                .map { it.name }
                .sorted())

        assertEquals(3, DBOperator.getAllCharacters()
            .count { it.name == "Terminator" })
        DBOperator.deleteAllCharactersOfUserFromSession(playerIds["Vasia"]!!, sId2)
        assertEquals(1, DBOperator.getAllCharacters()
            .count { it.name == "Terminator" })
        assertEquals(0, DBOperator.getAllCharacters()
            .count { it.name == "Heffalump" })
        assertEquals(1, DBOperator.getAllCharactersOfUser(playerIds["Vasia"]!!).count())
        assertEquals(0, DBOperator.getAllCharactersOfUserInSession(playerIds["Vasia"]!!, sId2).count())

        DBOperator.deleteAllSessions()
        assert(DBOperator.getAllSessionsWithUser(playerIds["Clara"]!!).isEmpty())
    }

    @AfterEach
    fun clearDatabase() {
        DBOperator.deleteAllSessions()
        DBOperator.getAllMaps().forEach { DBOperator.deleteMapByID(it.id) }
        DBOperator.getAllUsers().forEach { DBOperator.deleteUserByID(it.id) }
        DBOperator.deleteAllTextures()
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
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
        DBOperator.addPicture("$picturesFolder/avatar01.png")
        DBOperator.addPicture("$picturesFolder/avatar02.png")
        DBOperator.addPicture("$picturesFolder/avatar03.png")
        val avatarIds = DBOperator.getAllPictures().map { it.id }

        assertEquals("Vasia",
            DBOperator.addUser("Vasia", "vasia@mail.ru", "vasia12345", avatarIds[0])
                .login)
        assertEquals("petya@yandex.ru",
            DBOperator.addUser("Petya", "petya@yandex.ru","petya09876", avatarIds[1])
                .email)
        assertEquals(avatarIds[1],
            DBOperator.addUser("Clara", "clara@gmail.com","zmxncbva", avatarIds[1])
                .avatarID)
        DBOperator.addUser("Dendy", "dendy100100_0101@404.com","zmxncbva")

        assertThrows<IllegalArgumentException> { DBOperator.addUser("12345", "12345@mail.ru","abc") }
        assertThrows<IllegalArgumentException> { DBOperator.addUser("Clara", "clara2@gmail.com","loginalreadyexists", avatarIds[2]) }
        assertThrows<IllegalArgumentException> { DBOperator.addUser("Kim", "kim@mail.ru","비밀번호에잘못된문자가있습니다") }

        assertThrows<IllegalArgumentException> { DBOperator.addUser("12346", "petya@yandex.ru","emailalreadyexists") }
        assertThrows<IllegalArgumentException> { DBOperator.addUser("Wendy", "h@ck3r@O_o.|&|","emailincorrect", avatarIds[1]) }
        assertThrows<IllegalArgumentException> { DBOperator.addUser("Kim", "kimmail.ru","emailincorrect") }

        val users = DBOperator.getAllUsers()
        assert(users.any { it.login == "Vasia" })
        assert(users.any { it.login == "Clara" })
        assert(users.any { it.login == "Dendy" })

        val userVasia = DBOperator.getUserByLogin("Vasia")
        assertNotNull(userVasia)
        assertEquals("Vasia", userVasia!!.login)
        assertEquals("vasia@mail.ru", userVasia.email)
        assertEquals(avatarIds[0], userVasia.avatarID)
        assertEquals("Vasia", DBOperator.getUserByID(userVasia.id)?.login)
        assertEquals("Vasia", DBOperator.getUserByEmail(userVasia.email)?.login)
        assertNull(DBOperator.getUserByID(DBOperator.getAllUsers().maxOf { it.id } + 1u))

        assertNull(DBOperator.getUserByLogin("Dendy")!!.avatarID)

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

        DBOperator.addMap(filePath)
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
    fun sampleAvatarTest() {
        DBOperator.addPicture("$picturesFolder/avatar01.png")
        DBOperator.addPicture("$picturesFolder/avatar02.png")
        DBOperator.addPicture("$picturesFolder/avatar03.png")
        DBOperator.addPicture("$picturesFolder/avatar04.png") // примечание: avatar04, 05 и 06 и 07 на самом деле нет
        DBOperator.addPicture("$picturesFolder/avatar05.png")
        DBOperator.addPicture("$picturesFolder/avatar06.png")
        DBOperator.addPicture("$picturesFolder/avatar07.png")

        val pictureIds = DBOperator.getAllPictures().map { it.id }

        DBOperator.addUser("Vasia", "vasia@mail.ru", "vasia12345", pictureIds[0])
        DBOperator.addUser("Petya", "petya@gmail.com","petya09876", pictureIds[1])
        DBOperator.addUser("Clara", "clara@yandex.ru","zmxncbva", pictureIds[2])

        assertEquals(pictureIds[3], DBOperator.addUser("Dendy", "dendy@yahoo.com","zmxncbva", pictureIds[3]).avatarID)

        assertEquals(pictureIds[0], DBOperator.getUserByLogin("Vasia")?.avatarID)
        assertEquals(pictureIds[2], DBOperator.getUserByEmail("clara@yandex.ru")?.avatarID)

        var unusedPic = pictureIds.first {
            DBOperator.getPictureByID(it)?.pathToFile == "$picturesFolder/avatar05.png"
        }
        assertEquals(unusedPic, DBOperator.getPictureByPath("$picturesFolder/avatar05.png")?.id)

        // картинка используется, поэтому её нельзя удалить
        assertFalse(DBOperator.deletePictureById(DBOperator.getPictureByPath("$picturesFolder/avatar02.png")!!.id))
        assertTrue(DBOperator.deletePictureById(unusedPic))
        assertNull(DBOperator.getPictureByID(unusedPic))
        assertNull(DBOperator.getPictureByPath("$picturesFolder/avatar05.png"))

        DBOperator.deleteAllUnusedAvatars()
        assertNull(DBOperator.getPictureByPath("$picturesFolder/avatar06.png"))
        assertNull(DBOperator.getPictureByPath("$picturesFolder/avatar07.png"))
        assertNotNull(DBOperator.getPictureByPath("$picturesFolder/avatar03.png"))

        DBOperator.deleteUserByID(DBOperator.getUserByLogin("Clara")!!.id)
        DBOperator.deleteAllUnusedAvatars()
        assertNull(DBOperator.getPictureByPath("$picturesFolder/avatar03.png"))
    }

    @Test
    fun sampleSessionTest() {
        val mapFileName = sampleMapFiles[1]
        DBOperator.addPicture("$picturesFolder/avatar01.png")
        DBOperator.addPicture("$picturesFolder/avatar02.png")
        DBOperator.addPicture("$picturesFolder/avatar03.png")
        val avatarIds = DBOperator.getAllPictures().map { it.id }

        DBOperator.addUser("Vasia", "vasia@mail.ru", "vasia12345", avatarIds[0])
        DBOperator.addUser("Petya", "petya@gmail.com","petya09876", avatarIds[1])
        DBOperator.addUser("Clara", "clara@yandex.ru","zmxncbva", avatarIds[2])
        DBOperator.addUser("Dendy", "dendy@yahoo.com","zmxncbva")

        val userIds = DBOperator
            .getAllUsers()
            .associateBy({ it.login }) { it.id }

        DBOperator.addMap(mapFileName)
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

        // TODO: исправить тесты, чтобы они соответствовали новому механизму хранения свойств

        DBOperator.addCharacter(userIds["Vasia"]!!, sId1, "Dragonosaur",
            avatarIds[0], 1, 2,
            BasicProperties(1, 2, 3, 4, 5, 6))
        DBOperator.addCharacter(userIds["Vasia"]!!, sId1, "Mad Professor",
            null, 1, 2)
        DBOperator.addCharacter(userIds["Vasia"]!!, sId2, "Terminator",
            null, 1, 3)
        DBOperator.addCharacter(userIds["Vasia"]!!, sId2, "Terminator",
            avatarIds[1], 1, 3)
        DBOperator.addCharacter(userIds["Petya"]!!, sId1, "Sensei",
            null, 2, 3,
            BasicProperties())
        DBOperator.addCharacter(userIds["Petya"]!!, sId3, "Kongzilla",
            null, 3, 4,
            BasicProperties(-1, 1, -1, 1, -1, 1))
        DBOperator.addCharacter(userIds["Petya"]!!, sId3, "Hippoceros",
            null, 3, 4)
        DBOperator.addCharacter(userIds["Vasia"]!!, sId2, "Heffalump",
            avatarIds[2], 5, 3)
        DBOperator.addCharacter(userIds["Vasia"]!!, sId3, "Terminator",
            null, 1, 3)
        DBOperator.addCharacter(userIds["Clara"]!!, sId2, "Dragonosaur",
            avatarIds[1], 5)
        DBOperator.addCharacter(userIds["Clara"]!!, sId3, "Jabberwock")

        // returned characterInfo testing
        val tigerrat = DBOperator.addCharacter(userIds["Clara"]!!, sId2, "Tigerrat",
            avatarIds[2], 9000, 4,
            BasicProperties(1, -2, 3, -4, 5, -6))
        assertEquals("Tigerrat", tigerrat.name)
        assertEquals(Pair(9000, 4), Pair(tigerrat.row, tigerrat.col))
        assertEquals(sId2, tigerrat.sessionId)
        assertEquals(avatarIds[2], tigerrat.avatarId)
        assertEquals(BasicProperties(1, -2, 3, -4, 5, -6), tigerrat.basicProperties)

        // basic tests
        assert(DBOperator.getAllCharacters()
            .count { it.name == "Dragonosaur" } == 2)
        assert(DBOperator.getAllCharacters()
            .count { it.name == "Terminator" } == 3)
        assert(DBOperator.getAllCharacters()
            .filter { it.name == "Terminator" }
            .all { it.userId == userIds["Vasia"]!! })

        // characters of a concrete player
        val petyaCharacters = DBOperator.getAllCharactersOfUser(userIds["Petya"]!!)
        assertEquals(3, petyaCharacters.count())
        assertEquals(listOf("Hippoceros", "Kongzilla", "Sensei"), petyaCharacters.map { it.name }.sorted())
        assertEquals(Pair(3, 4),
            petyaCharacters.first { it.name == "Kongzilla" }
                .let { Pair(it.row, it.col) })
        petyaCharacters.first { it.name == "Hippoceros" }
            .let { hippo ->
                DBOperator.getCharacterByID(hippo.id)
                .let {
                    assertNotNull(it)
                    assert(it!!.name == "Hippoceros")
                    assert(it.userId == userIds["Petya"])
                } }

        assertEquals(0, DBOperator.getAllCharactersOfUser(userIds["Dendy"]!!).count())

        // characters of a concrete session
        val session3Characters = DBOperator.getAllCharactersInSession(sId3)
        assertEquals(4, session3Characters.count())
        assertEquals(2, session3Characters.count { it.userId == userIds["Petya"]!! })
        assertEquals(Pair(0, 0), session3Characters.first { it.name == "Jabberwock" }
            .let { Pair(it.row, it.col) })
        assertEquals(listOf("Hippoceros", "Jabberwock", "Kongzilla", "Terminator"),
            session3Characters.map { it.name }.sorted())
        assertEquals(listOf("Jabberwock"),
            session3Characters
                .filter { it.userId == userIds["Clara"]!! }
                .map { it.name })

        // get all characters of user in session
        assertEquals(listOf<CharacterInfo>(),
            DBOperator.getAllCharactersOfUserInSession(userIds["Petya"]!!, sId2))
        assertEquals(listOf("Heffalump", "Terminator", "Terminator"),
            DBOperator.getAllCharactersOfUserInSession(userIds["Vasia"]!!, sId2)
                .map { it.name }
                .sorted())
        assertEquals(listOf("Hippoceros", "Kongzilla"),
            DBOperator.getAllCharactersOfUserInSession(userIds["Petya"]!!, sId3)
                .map { it.name }
                .sorted())

        // trying to move a characters & change properties
        val dragonosaurId: UInt
        assertEquals(Pair(1, 2),
            DBOperator.getAllCharactersOfUserInSession(userIds["Vasia"]!!, sId1)
                .first { it.name == "Dragonosaur" }
                .also { dragonosaurId = it.id }
                .let { Pair(it.row, it.col) })

        DBOperator.moveCharacter(dragonosaurId, 3, 5)
        assertEquals(Pair(3, 5),
            DBOperator.getAllCharactersOfUserInSession(userIds["Vasia"]!!, sId1)
                .first { it.name == "Dragonosaur" }
                .let { Pair(it.row, it.col) })

        val propNames = characterPropertiesList.keys.toList()
        assertEquals(characterPropertiesList[propNames[0]]!!.invoke(DBOperator.getCharacterByID(dragonosaurId)!!.basicProperties),
            DBOperator.getCharacterProperty(dragonosaurId, propNames[0]))
        DBOperator.setCharacterProperty(dragonosaurId, propNames[0], 1000)
        assertEquals(1000, DBOperator.getCharacterProperty(dragonosaurId, propNames[0]))
        DBOperator.setCharacterProperty(dragonosaurId, propNames[0], -500)
        assertEquals(-500, DBOperator.getCharacterProperty(dragonosaurId, propNames[0]))
        DBOperator.resetCharacterPropertyToDefault(dragonosaurId, propNames[0])
        assertEquals(characterPropertiesList[propNames[0]]!!.invoke(DBOperator.getCharacterByID(dragonosaurId)!!.basicProperties),
            DBOperator.getCharacterProperty(dragonosaurId, propNames[0]))

        assertEquals(avatarIds[0], DBOperator.getCharacterByID(dragonosaurId)?.avatarId)
        assertNull(DBOperator.getAllCharactersOfUser(userIds["Petya"]!!).first().avatarId)

        // trying to delete
        DBOperator.deleteCharacterById(dragonosaurId)
        assertEquals(listOf("Mad Professor"),
            DBOperator.getAllCharactersOfUserInSession(userIds["Vasia"]!!, sId1)
                .map { it.name })
        assertNull(DBOperator.getCharacterByID(dragonosaurId))

        DBOperator.addCharacter(userIds["Dendy"]!!, sId1,
            "Bandersnatch", null, 6, 7)
        DBOperator.addCharacter(userIds["Dendy"]!!, sId3,
            "Kraken", avatarIds[1], 8)
        val fantomasId = DBOperator.addCharacter(userIds["Dendy"]!!, sId2,
            "Fantômas", null, -1, -3).id

        // trying to delete user
        DBOperator.deleteUserByID(userIds["Petya"]!!) // all Petya characters removed from sessions

        assertEquals(listOf("Bandersnatch", "Mad Professor"),
            DBOperator.getAllCharactersInSession(sId1)
                .map { it.name }
                .sorted())
        assertEquals(listOf("Bandersnatch", "Fantômas", "Kraken"),
            DBOperator.getAllCharactersOfUser(userIds["Dendy"]!!)
                .map { it.name }
                .sorted())
        assertEquals(listOf("Fantômas"),
            DBOperator.getAllCharactersOfUserInSession(userIds["Dendy"]!!, sId2)
                .map { it.name })
        assertEquals(Pair(8, 0),
            DBOperator.getAllCharacters()
                .first { it.name == "Kraken" }
                .let { Pair(it.row, it.col) })

        // some more properties tests
        assertEquals(characterPropertiesList[propNames[1]]!!.invoke(DBOperator.getCharacterByID(fantomasId)!!.basicProperties),
            DBOperator.getCharacterProperty(fantomasId, propNames[1]))
        DBOperator.setCharacterProperty(fantomasId, propNames[1], 3)
        assertEquals(3, DBOperator.getCharacterProperty(fantomasId, propNames[1]))
        assertEquals(characterPropertiesList[propNames[2]]!!.invoke(DBOperator.getCharacterByID(fantomasId)!!.basicProperties),
            DBOperator.getCharacterProperty(fantomasId, propNames[2]))
        DBOperator.setCharacterProperty(fantomasId, propNames[2], -4)
        assertEquals(-4, DBOperator.getCharacterProperty(fantomasId, propNames[2]))
        assertEquals(3, DBOperator.getCharacterProperty(fantomasId, propNames[1]))
        DBOperator.resetCharacterPropertyToDefault(fantomasId, propNames[1])
        assertEquals(-4, DBOperator.getCharacterProperty(fantomasId, propNames[2]))
        assertEquals(characterPropertiesList[propNames[1]]!!.invoke(DBOperator.getCharacterByID(fantomasId)!!.basicProperties),
            DBOperator.getCharacterProperty(fantomasId, propNames[1]))

        // trying to delete session
        DBOperator.deleteSessionByID(sId1)
        assertNull(DBOperator.getSessionByID(sId1))
        assertThrows<IllegalArgumentException> { DBOperator.getAllUsersInSession(sId1) }
        assertThrows<NoSuchElementException> { DBOperator.getAllCharactersInSession(sId1).first() }
        assertEquals(listOf("Fantômas", "Kraken"),
            DBOperator.getAllCharactersOfUser(userIds["Dendy"]!!)
                .map { it.name }
                .sorted())

        assertEquals(3, DBOperator.getAllCharacters()
            .count { it.name == "Terminator" })
        DBOperator.deleteAllCharactersOfUserFromSession(userIds["Vasia"]!!, sId2)
        assertEquals(1, DBOperator.getAllCharacters()
            .count { it.name == "Terminator" })
        assertEquals(0, DBOperator.getAllCharacters()
            .count { it.name == "Heffalump" })
        assertEquals(1, DBOperator.getAllCharactersOfUser(userIds["Vasia"]!!).count())
        assertEquals(0, DBOperator.getAllCharactersOfUserInSession(userIds["Vasia"]!!, sId2).count())

        // trying to delete all
        DBOperator.deleteAllSessions()
        assert(DBOperator.getAllSessionsWithUser(userIds["Clara"]!!).isEmpty())
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
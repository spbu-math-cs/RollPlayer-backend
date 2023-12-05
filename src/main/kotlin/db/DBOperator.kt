package db

import kotlin.collections.Map
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.*
import java.time.Instant
import java.util.Random

const val dbPath = "./data/roll_player"
const val dbTestPath = "./data/test_db"
const val texturesFolder = "./resources/textures"
const val tilesetsFolder = "./resources/tilesets"
const val mapsFolder = "./resources/maps"
const val picturesFolder = "./resources/pictures"

object DBOperator {
    // =================
    // SERVICE FUNCTIONS
    // =================

    private fun extractFileName(fileName: String) =
        fileName
            .removeSuffix(".db")
            .removeSuffix(".mv")
            .removeSuffix(".trace")
            .removeSuffix(".sql")
            .removeSuffix(".jpg")
            .removeSuffix(".png")
            .removeSuffix(".bmp")
            .removeSuffix(".json")
            .removeSuffix(".tmj")
            .removeSuffix(".tsj")

    private fun initTextures(textureDir: File) {
        textureDir.listFiles()?.forEach { addTexture(it.path) }
    }

    private fun initTilesets(tilesetDir: File) {
        tilesetDir.listFiles()?.forEach { addTileset(it.path) }
    }

    private fun initMaps(mapDir: File) {
        mapDir.listFiles()?.forEach { addMap(it.path) }
    }

    private fun initAvatars(avatarDir: File) {
        avatarDir.listFiles()?.forEach { addPicture(it.path) }
    }

    private fun createDatabase(filePath: String, initTables: Boolean = false) {
        val exists = File("$filePath.mv.db").isFile

        Database.connect("jdbc:h2:$filePath")

        if (!exists) {
            // create DB schema
            transaction {
                SchemaUtils.create(UserTable)
                SchemaUtils.create(TextureTable)
                SchemaUtils.create(TilesetTable)
                SchemaUtils.create(MapTable)
                SchemaUtils.create(PictureTable)
                SchemaUtils.create(SessionTable)
                SchemaUtils.create(CharacterTable)
                SchemaUtils.create(PropertyTable)
            }
        }

        // создать папки с текстурами, тайлсетами и картами, если их нет
        val textureDir = File(texturesFolder)
        val tilesetDir = File(tilesetsFolder)
        val mapDir = File(mapsFolder)
        val pictureDir = File(picturesFolder)
        textureDir.mkdirs()
        tilesetDir.mkdirs()
        mapDir.mkdirs()
        pictureDir.mkdirs()

        if (initTables) {
            initTextures(textureDir)
            initTilesets(tilesetDir)
            initMaps(mapDir)
            initAvatars(pictureDir)
        }
    }

    fun connectOrCreate(initTables: Boolean = false) =
        createDatabase(dbPath, initTables)

    fun createDBForTests(initTables: Boolean = false) =
        createDatabase(dbTestPath, initTables)

    private fun deleteFileIfExists(filePath: String) =
        (File(filePath)).let { file ->
            if (file.exists())
                file.delete()
        }

    private fun deleteDBByFileName(fileName: String) {
        deleteFileIfExists("$fileName.mv.db")
        deleteFileIfExists("$fileName.trace.db")
    }

    fun deleteDatabase() = deleteDBByFileName(dbPath)

    fun deleteTestDatabase() = deleteDBByFileName(dbTestPath)

    // =============
    // GET FUNCTIONS
    // =============

    fun getAllTextures() = transaction { TextureData.all().map { it.raw() } }
    fun getAllTilesets() = transaction { TilesetData.all().map { it.raw() } }
    fun getAllMaps() = transaction { MapData.all().map { it.raw() } }
    fun getAllPictures() = transaction { PictureData.all().map { it.raw() } }
    fun getAllUsers() = transaction { UserData.all().map { it.raw() } }

    fun getAllSessions() = transaction { SessionData.all().map { it.raw() } }
    fun getActiveSessions() = transaction {
        SessionData.find(SessionTable.active eq true)
            .map { it.raw() }
    }

    fun getAllCharacters() = transaction {
        CharacterData.all()
            .orderBy(CharacterTable.id to SortOrder.ASC)
            .map { it.raw() }
    }

    fun getUserByID(id: UInt) = transaction { UserData.findById(id.toInt())?.raw() }
    fun getTextureByID(id: UInt) = transaction { TextureData.findById(id.toInt())?.raw() }
    fun getTilesetByID(id: UInt) = transaction { TilesetData.findById(id.toInt())?.raw() }
    fun getMapByID(id: UInt) = transaction { MapData.findById(id.toInt())?.raw() }
    fun getPictureByID(id: UInt) = transaction { PictureData.findById(id.toInt())?.raw() }
    fun getSessionByID(id: UInt) = transaction { SessionData.findById(id.toInt())?.raw() }
    fun getCharacterByID(id: UInt) = transaction { CharacterData.findById(id.toInt())?.raw() }

    fun getUserByLogin(login: String): UserInfo? = transaction {
        UserData.find(UserTable.login eq login)
            .map { it.raw() }
            .firstOrNull()
    }

    fun getUserByEmail(email: String): UserInfo? = transaction {
        UserData.find(UserTable.email eq email)
            .map { it.raw() }
            .firstOrNull()
    }

    fun getAllUsersInSession(sessionId: UInt) = transaction {
        SessionData.findById(sessionId.toInt())?.users?.map { it.raw() }
            ?: throw IllegalArgumentException("Session #$sessionId does not exist")
    }

    fun getAllCharactersInSession(sessionId: UInt) = transaction {
        CharacterData.find(CharacterTable.sessionID eq sessionId.toInt())
            .orderBy(CharacterTable.id to SortOrder.ASC)
            .map { it.raw() }
    }

    fun getAllCharactersOfUser(userId: UInt) = transaction {
        CharacterData.find(
            CharacterTable.userID eq userId.toInt()
        )
            .orderBy(CharacterTable.id to SortOrder.ASC)
            .map { it.raw() }
    }

    fun getAllCharactersOfUserInSession(userId: UInt, sessionId: UInt) = transaction {
        CharacterData.find(
            CharacterTable.userID eq userId.toInt() and
                    (CharacterTable.sessionID eq sessionId.toInt())
        )
            .orderBy(CharacterTable.id to SortOrder.ASC)
            .map { it.raw() }
    }

    fun getAllSessionsWithUser(userId: UInt) = transaction {
        UserData.findById(userId.toInt())?.sessions?.distinct()?.map { it.raw() }
            ?: throw IllegalArgumentException("User #$userId does not exist")
    }

    private fun getPropertyNameDataNoTransaction(propName: String): PropertyNameData =
        PropertyNameData
            .find(PropertyNameTable.name eq propName).firstOrNull()
            ?: PropertyNameData.new {
                name = propName
            }

    // TODO: Рефакторинг механизма хранения свойств

    fun getPropertyOfCharacter(characterId: UInt, propName: String) = transaction {
        if (!characterPropertiesList.containsKey(propName))
            return@transaction null
        val propNameId = getPropertyNameDataNoTransaction(propName).id.value
        PropertyData.find(PropertyTable.characterID eq characterId.toInt() and
                (PropertyTable.nameID eq propNameId))
            .firstOrNull()
            ?.value ?: resetCharacterPropertyNoTransaction(
                CharacterData.findById(characterId.toInt()) ?: return@transaction null,
                propName)
    }

    fun getPropertiesOfCharacter(characterId: UInt) = transaction {
        PropertyData.find(PropertyTable.characterID eq characterId.toInt())
            .associateBy({ it.nameData.name }) { it.value }
    }

    // ================
    // CREATE FUNCTIONS
    // ================

    fun addTexture(pathToFile: String) = transaction {
        TextureData.find(TextureTable.pathToFile eq pathToFile)
            .firstOrNull()
            .let { if (it != null) return@transaction it.raw() }

        TextureData.new {
            this.pathToFile = pathToFile
        }.raw()
    }

    fun addTileset(pathToJson: String) = transaction {
        TilesetData.find(TilesetTable.pathToJson eq pathToJson)
            .firstOrNull()
            .let { if (it != null) return@transaction it.raw() }

        TilesetData.new {
            this.pathToJson = pathToJson
        }.raw()
    }

    fun addMap(pathToJson: String) = transaction {
        MapData.find(MapTable.pathToJson eq pathToJson)
            .firstOrNull()
            .let { if (it != null) return@transaction it.raw() }

        MapData.new {
            this.pathToJson = pathToJson
        }.raw()
    }

    fun addPicture(pathToFile: String) = transaction {
        PictureData.find(PictureTable.pathToFile eq pathToFile)
            .firstOrNull()
            .let { if (it != null) return@transaction it.raw() }

        PictureData.new {
            this.pathToFile = pathToFile
        }.raw()
    }

    fun addSession(mapID: UInt = 1u, active: Boolean = false, started: Instant = Instant.now(), whoCanMove: Int = -1) =
        transaction {
            SessionData.new {
                map = MapData.findById(mapID.toInt())
                    ?: throw IllegalArgumentException("Map #${mapID} does not exist")
                this.active = active
                this.started = started
                this.prevCharacterId = whoCanMove
            }.raw()
        }

    fun addCharacter(
        userId: UInt,
        sessionId: UInt,
        name: String,
        avatarId: UInt? = null,
        row: Int = 0,
        col: Int = 0,
        basicProperties: BasicProperties = BasicProperties(),
        properties: Map<String, Int> = mapOf()
    ) = transaction {
        val newCharacter = CharacterData.new {
            session = SessionData.findById(sessionId.toInt())
                ?: throw IllegalArgumentException("Session #$sessionId does not exist")
            user = UserData.findById(userId.toInt())
                ?: throw IllegalArgumentException("User #$userId does not exist")
            this.name = name

            // примечание: если картинка не найдена в базе, устанавливает её в null
            this.avatar = if (avatarId != null) PictureData.findById(avatarId.toInt()) else null

            this.row = row
            this.col = col

            this.strength = basicProperties.strength
            this.dexterity = basicProperties.dexterity
            this.constitution = basicProperties.constitution
            this.intelligence = basicProperties.intelligence
            this.wisdom = basicProperties.wisdom
            this.charisma = basicProperties.charisma
        }

        for ((propName, propFunc) in characterPropertiesList) {
            PropertyData.new {
                this.character = newCharacter
                this.nameData = getPropertyNameDataNoTransaction(propName)
                if (properties.containsKey(propName))
                    this.value = properties[propName]!!
                else this.value = propFunc(basicProperties)
            }
        }

        newCharacter.raw()
    }

    // =================
    // USER MANIPULATION
    // =================

    private const val MIN_PASSWORD_CHARACTERS = 8
    private const val PASSWORD_SPECIAL_CHARS = ",.!@#$%^&*()\":;\'_-+=[]|{}~`<>?/\\"
    private const val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z.-]+\$"
    private val PASSWORD_CHAR_LIST = sequence<Char> {
        yieldAll('A'..'Z')
        yieldAll('a'..'z')
        yieldAll('0'..'9')
        yieldAll(PASSWORD_SPECIAL_CHARS.toList())
    }.toList()
    private const val PASSWORD_HASH_MODULUS = 2147483647L // простое число; модуль должен быть простым

    private fun failOnInvalidLogin(login: String) {
        if (login.isBlank())
            throw IllegalArgumentException("Cannot create user with blank login")

        if (!UserData.find(UserTable.login eq login).empty())
            throw IllegalArgumentException("User with login `$login` already exists")
    }

    private fun failOnInvalidEmail(email: String) {
        if (!email.matches(EMAIL_REGEX.toRegex()))
            throw IllegalArgumentException("Email does not match regex $EMAIL_REGEX")
        if (!UserData.find(UserTable.email eq email).empty())
            throw IllegalArgumentException("User with email `$email` already exists")
    }

    private fun failOnInvalidPassword(password: String) {
        if (password.length < MIN_PASSWORD_CHARACTERS)
            throw IllegalArgumentException("User password must have at least $MIN_PASSWORD_CHARACTERS characters")

        password.forEach {
            if (it !in PASSWORD_CHAR_LIST)
                throw IllegalArgumentException(
                    "Invalid character `$it` in password:" +
                            " password must contain only characters" +
                            " A-Z a-z 0-9 $PASSWORD_SPECIAL_CHARS"
                )
        }
    }

    // для тестирования
    fun checkLoginAvailability(login: String) = transaction {
        try {
            failOnInvalidLogin(login)
            true
        } catch (e: Exception) {
            false
        }
    }

    // для тестирования
    fun checkEmailAvailability(email: String) = transaction {
        try {
            failOnInvalidEmail(email)
            true
        } catch (e: Exception) {
            false
        }
    }

    // для тестирования
    fun checkPasswordValidity(password: String) = try {
        failOnInvalidPassword(password)
        true
    } catch (_: Exception) {
        false
    }

    fun hashPassword(password: String, pswInit: Int, pswFactor: Int): Int {
        fun mathMod(l: Long, m: Long) =
            (l % m).let {
                if (it < 0) m - it
                else it
            }

        var hash: Long = pswInit.toLong()
        for (ch in password)
            hash = mathMod((hash * pswFactor + ch.code), PASSWORD_HASH_MODULUS)

        return hash.toInt()
    }

    fun addUser(login: String, email: String, password: String, avatarId: UInt? = null) = transaction {
        failOnInvalidLogin(login)
        failOnInvalidEmail(email)
        failOnInvalidPassword(password)

        val rand = Random()
        val pswInit = rand.nextInt(PASSWORD_HASH_MODULUS.toInt())
        val pswFactor = rand.nextInt(PASSWORD_HASH_MODULUS.toInt())

        UserData.new {
            this.login = login
            this.email = email
            this.passwordHash = hashPassword(password, pswInit, pswFactor)
            this.pswHashInitial = pswInit
            this.pswHashFactor = pswFactor
            this.avatar = if (avatarId != null) PictureData.findById(avatarId.toInt()) else null
        }.raw()
    }

    fun checkUserPassword(userId: UInt, password: String) = transaction {
        val user = UserData.findById(userId.toInt())
            ?: throw IllegalArgumentException("User #$userId does not exist")

        hashPassword(password, user.pswHashInitial, user.pswHashFactor) == user.passwordHash
    }

    fun updateUserLogin(userId: UInt, newLogin: String) = transaction {
        val user = UserData.findById(userId.toInt())
            ?: throw IllegalArgumentException("User #$userId does not exist")

        failOnInvalidLogin(newLogin)

        user.login = newLogin
    }

    fun updateUserEmail(userId: UInt, newEmail: String) = transaction {
        val user = UserData.findById(userId.toInt())
            ?: throw IllegalArgumentException("User #$userId does not exist")

        failOnInvalidEmail(newEmail)

        user.email = newEmail
    }

    fun updateUserPassword(userId: UInt, newPassword: String) = transaction {
        val user = UserData.findById(userId.toInt())
            ?: throw IllegalArgumentException("User #$userId does not exist")

        failOnInvalidPassword(newPassword)

        user.passwordHash = hashPassword(newPassword, user.pswHashInitial, user.pswHashFactor)
    }

    // ====================
    // SESSION MANIPULATION
    // ====================

    fun setSessionActive(sessionId: UInt, active: Boolean) = transaction {
        (SessionData.findById(sessionId.toInt())
            ?: throw IllegalArgumentException("Session #$sessionId does not exist"))
            .active = active
    }

    fun updateSession(sessionInfo: SessionInfo) = transaction {
        (SessionData.findById(sessionInfo.id.toInt())
            ?: throw IllegalArgumentException("Session #${sessionInfo.id} does not exist"))
            .apply {
                // Возможно, карту менять не нужно (выполнять лишние действия)?
                map = MapData.findById(sessionInfo.mapID.toInt())
                    ?: throw IllegalArgumentException("Map #${sessionInfo.mapID} does not exist")
                started = sessionInfo.started.toJavaInstant()
                active = sessionInfo.active
                prevCharacterId = sessionInfo.prevCharacterId
            }.raw()
    }

    // ======================
    // CHARACTER MANIPULATION
    // ======================

    fun moveCharacter(characterId: UInt, newRow: Int, newCol: Int) = transaction {
        CharacterData
            .findById(characterId.toInt())
            ?.apply {
                row = newRow
                col = newCol
            }?.raw()
    }

    private fun setCharacterPropertyNoTransaction(character: CharacterData, propName: String, propValue: Int) {
        val propNameDataString = getPropertyNameDataNoTransaction(propName)
        PropertyData.find(PropertyTable.characterID eq character.id.value and
                (PropertyTable.nameID eq propNameDataString.id.value))
            .firstOrNull()
            ?.apply {
                this.value = propValue
            } ?: PropertyData.new {
                this.character = character
                this.nameData = propNameDataString
                this.value = propValue
            }
    }

    private fun resetCharacterPropertyNoTransaction(character: CharacterData, propName: String): Int? {
        if (!characterPropertiesList.containsKey(propName))
            return null
        val default = characterPropertiesList[propName]!!(character.getBasicProperties())
        setCharacterPropertyNoTransaction(character, propName, default)
        return default
    }

    fun setCharacterProperty(characterId: UInt, propName: String, propValue: Int) = transaction {
        if (!characterPropertiesList.containsKey(propName))
            return@transaction false
        setCharacterPropertyNoTransaction(
            CharacterData.findById(characterId.toInt())
                ?: return@transaction false,
            propName, propValue)
        true
    }

    fun resetCharacterPropertyToDefault(characterId: UInt, propName: String) = transaction {
        if (!characterPropertiesList.containsKey(propName))
            return@transaction false
        val character = CharacterData.findById(characterId.toInt())
            ?: return@transaction false
        setCharacterPropertyNoTransaction(character, propName, characterPropertiesList[propName]!!(character.getBasicProperties()))
        true
    }

    fun updateCharacterProperties(characterId: UInt, propertiesToUpdate: Map<String, Int>) = transaction {
        CharacterData.findById(characterId.toInt())
            ?.apply {
                for ((propName, propValue) in propertiesToUpdate.filter { characterPropertiesList.containsKey(it.key) }) {
                    setCharacterPropertyNoTransaction(this, propName, propValue)
                }
            }?.raw() ?: throw IllegalArgumentException("Character #$characterId does not exist")
    }

    // ================
    // DELETE FUNCTIONS
    // ================

    fun deleteAllTextures() = transaction {
        TextureData.all()
            .forEach { it.delete() }
    }

    fun deleteAllSessions() = transaction {
        SessionData.all()
            .forEach { it.delete() }
    }

    // ВНИМАНИЕ: также этот пользователь выйдет из всех сессий
    fun deleteUserByID(id: UInt): Boolean = transaction {
        UserData.findById(id.toInt())
            ?.delete() ?: return@transaction false
        true
    }

    fun deleteTextureByID(id: UInt): Boolean = transaction {
        TextureData.findById(id.toInt())
            ?.delete() ?: return@transaction false
        true
    }

    fun deleteTilesetByID(id: UInt): Boolean = transaction {
        TilesetData.findById(id.toInt())
            ?.delete() ?: return@transaction false
        true
    }

    // ВНИМАНИЕ: также удалит все сессии на этой карте
    // Это делается для ненарушения ссылочной целостности
    fun deleteMapByID(id: UInt): Boolean = transaction {
        MapData.findById(id.toInt())
            ?.delete() ?: return@transaction false
        true
    }

    fun deleteSessionByID(id: UInt): Boolean = transaction {
        SessionData.findById(id.toInt())
            ?.delete() ?: return@transaction false
        true
    }

    fun deleteCharacterById(id: UInt): Boolean = transaction {
        CharacterData.findById(id.toInt())
            ?.delete() ?: return@transaction false
        true
    }

    fun deleteAllUnusedAvatars() = transaction {
        PictureData.all()
            .forEach {
                if (CharacterData.find(CharacterTable.avatarID eq it.id).empty() &&
                    UserData.find(CharacterTable.avatarID eq it.id).empty())
                    it.delete()
            }
    }

    fun deleteAvatarByPath(avatarPath: String) = transaction {
        PictureData.find(PictureTable.pathToFile eq avatarPath)
            .firstOrNull()
            .let {
                if (it == null)
                    return@transaction false

                if (!CharacterData.find(CharacterTable.avatarID eq it.id).empty() ||
                    !UserData.find(CharacterTable.avatarID eq it.id).empty())
                    throw IllegalAccessException("Cannot remove avatar file $avatarPath from the DB, because someone's using it")

                it.delete()
                return@transaction true
            }
    }

    fun deleteAllCharactersOfUserFromSession(uId: UInt, sId: UInt) = transaction {
        CharacterData.find(
            CharacterTable.sessionID eq sId.toInt() and
                    (CharacterTable.userID eq uId.toInt())
        ).forEach {
            it.delete()
        }
    }
}

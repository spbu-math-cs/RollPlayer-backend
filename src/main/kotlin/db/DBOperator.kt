package db

import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.*
import java.util.Random

const val dbPath = "./data/roll_player"
const val dbTestPath = "./data/test_db"
const val mapsFolder = "./maps"
const val texturesFolder = "./textures"

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

    private fun createDatabase(filePath: String) {
        val exists = File("$filePath.mv.db").isFile

        Database.connect("jdbc:h2:$filePath")

        if (!exists) {
            // create DB schema
            transaction {
                SchemaUtils.create(UserTable)
                SchemaUtils.create(TextureTable)
                SchemaUtils.create(MapTable)
                SchemaUtils.create(SessionTable)
                SchemaUtils.create(SessionPlayerTable)
            }
        }

        // создать папки с картами и текстурами, если их нет
        val mapDir = File(mapsFolder)
        val txtrDir = File(texturesFolder)
        mapDir.mkdirs()
        txtrDir.mkdirs()
    }

    fun connectOrCreate() =
        createDatabase(dbPath)

    fun createDBForTests() =
        createDatabase(dbTestPath)

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

    fun getAllTextures() = transaction {
        TextureData.all()
            .map { it.raw() }
    }

    fun getAllUsers() = transaction {
        UserData.all()
            .map { it.raw() }
    }
    fun getAllMapInfos() = transaction { MapData.all().map { it.raw() } }
    fun getAllSessions() = transaction { SessionData.all().map { it.raw() } }
    fun getActiveSessions() = transaction {
        SessionData.find(SessionTable.active eq true)
            .map { it.raw() }
    }

    fun getUserByID(id: Int) = transaction { UserData.findById(id)?.raw() }
    fun getTextureByID(id: Int) = transaction { TextureData.findById(id)?.raw() }
    fun getMapByID(id: Int) = transaction { MapData.findById(id)?.raw() }
    fun getSessionByID(id: Int) = transaction { SessionData.findById(id)?.raw() }

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

    fun getUsersInSession(sessionId: Int) = transaction {
        SessionData.findById(sessionId)?.players?.map { it.raw() }
            ?: throw IllegalArgumentException("Session #$sessionId does not exist")
    }

    fun getPlayersGameStateOfSession(sessionId: Int) = transaction {
        SessionPlayerData.find(SessionPlayerTable.sessionID eq sessionId)
            .map { it.raw() }
    }

    fun getSessionsOfUser(userId: Int) = transaction {
        UserData.findById(userId)?.sessions?.map { it.raw() }
            ?: throw IllegalArgumentException("User #$userId does not exist")
    }

    // ================
    // CREATE FUNCTIONS
    // ================

    fun createNewMap(fileName: String, mapName: String) = transaction {
        val mapPath = "$mapsFolder/${extractFileName(fileName)}.json"
        val mapFile = File(mapPath)
        if (!mapFile.createNewFile())
            throw FileAlreadyExistsException(mapFile, reason = "Map $fileName already exists")
        if (!MapData.find(MapTable.pathToJson eq mapPath).empty())
            throw FileAlreadyExistsException(mapFile, reason = "Map $fileName already recorded in the database")

        @Language("JSON") val emptyMap = """
            {
                "name": "$mapName"
            }
        """.trimIndent()

        mapFile.writeText(emptyMap)

        return@transaction MapData.new {
            pathToJson = mapPath
        }.raw()
    }

    fun addTexture(textureInfo: TextureInfo) = transaction {
        if (!TextureData.find(TextureTable.pathToFile eq textureInfo.pathToFile).empty())
            throw IllegalArgumentException("Texture `${textureInfo.pathToFile}` already recorded in the database")

        TextureData.new {
            pathToFile = textureInfo.pathToFile
        }
    }

    fun addMap(mapInfo: MapInfo) = transaction {
        if (!MapData.find(MapTable.pathToJson eq mapInfo.pathToJson).empty())
            throw IllegalArgumentException("Map `${mapInfo.pathToJson}` already recorded in the database")

        MapData.new {
            pathToJson = mapInfo.pathToJson
        }
    }

    fun addSession(sessionInfo: SessionInfo) = transaction {
        SessionData.new {
            map = MapData.findById(sessionInfo.mapID)
                ?: throw IllegalArgumentException("Map #${sessionInfo.mapID} does not exist")
            active = sessionInfo.active
            started = sessionInfo.started
        }
    }

    // =================
    // USER MANIPULATION
    // =================

    private const val MIN_PASSWORD_CHARACTERS = 8
    private const val PASSWORD_SPECIAL_CHARS = ",.!@#$%^&*()\":;\'_-+=[]|{}~`<>?/\\"
    private const val  EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z.-]+\$"
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
            throw IllegalArgumentException("Cannot create user with blank login")
        if (!UserData.find(UserTable.email eq email).empty())
            throw IllegalArgumentException("User with login `$email` already exists")
    }

    private fun failOnInvalidPassword(password: String) {
        if (password.length < MIN_PASSWORD_CHARACTERS)
            throw IllegalArgumentException("User password must have at least $MIN_PASSWORD_CHARACTERS characters")

        password.forEach {
            if (it !in PASSWORD_CHAR_LIST)
                throw IllegalArgumentException("Invalid character `$it` in password:" +
                        " password must contain only characters" +
                        " A-Z a-z 0-9 $PASSWORD_SPECIAL_CHARS")
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
    fun checkPasswordValidity(password: String) = try {
        failOnInvalidPassword(password)
        true
    } catch (_: Exception) { false }

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

    fun addUser(login: String, email: String, password: String) = transaction {
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
        }.id
    }

    fun checkUserPassword(userId: Int, password: String) = transaction {
        val user = UserData.findById(userId)
            ?: throw IllegalArgumentException("User #$userId does not exist")

        hashPassword(password, user.pswHashInitial, user.pswHashFactor) == user.passwordHash
    }

    fun updateUserLogin(userId: Int, newLogin: String) = transaction {
        val user = UserData.findById(userId)
            ?: throw IllegalArgumentException("User #$userId does not exist")

        failOnInvalidLogin(newLogin)

        user.login = newLogin
    }

    fun updateUserEmail(userId: Int, newEmail: String) = transaction {
        val user = UserData.findById(userId)
            ?: throw IllegalArgumentException("User #$userId does not exist")

        failOnInvalidEmail(newEmail)

        user.email = newEmail
    }

    fun updateUserPassword(userId: Int, newPassword: String) = transaction {
        val user = UserData.findById(userId)
            ?: throw IllegalArgumentException("User #$userId does not exist")

        failOnInvalidPassword(newPassword)

        user.passwordHash = hashPassword(newPassword, user.pswHashInitial, user.pswHashFactor)
    }

    // ====================
    // SESSION MANIPULATION
    // ====================

    fun addPlayerToSession(sId: Int, uId: Int, x: Int = 0, y: Int = 0) = transaction {
        SessionPlayerData.new {
            session = SessionData.findById(sId)
                ?: throw IllegalArgumentException("Session #$sId does not exist")
            player = UserData.findById(uId)
                ?: throw IllegalArgumentException("User #$uId does not exist")
            xPos = x
            yPos = y
        }
    }

    fun setSessionActive(sessionId: Int, active: Boolean) = transaction {
        SessionData.findById(sessionId)
            .also { if (it == null) return@transaction false }
            ?.active = active
        true
    }

    fun movePlayer(sessionId: Int, playerId: Int, moveToX: Int, moveToY: Int) = transaction {
        SessionPlayerData.find(
            SessionPlayerTable.sessionID eq sessionId and
                    (SessionPlayerTable.playerID eq playerId)
        ).let {
            if (it.count() == 0L)
                throw IllegalArgumentException("Player #$playerId is not present in session #$sessionId or session does not exist")
            else it.first().let { rec ->
                rec.xPos = moveToX
                rec.yPos = moveToY
            }
        }
    }

    fun removePlayerFromSession(sId: Int, uId: Int) = transaction {
        SessionPlayerData.find(
            SessionPlayerTable.sessionID eq sId and
                    (SessionPlayerTable.playerID eq uId)
        ).let {
            if (it.count() == 0L) return@transaction false
            else it.first().delete()
        }
        true
    }

    // ================
    // DELETE FUNCTIONS
    // ================

    fun deleteAllTextures() = transaction {
        TextureData.all()
            .forEach { it.delete() }
    }

    fun deleteAllSessions() = transaction {
        SessionPlayerData.all()
            .forEach { it.delete() }
        SessionData.all()
            .forEach { it.delete() }
    }

    // ВНИМАНИЕ: также этот пользователь выйдет из всех сессий
    fun deleteUserByID(id: Int): Boolean = transaction {
        SessionPlayerData.find(SessionPlayerTable.playerID eq id)
            .forEach { it.delete() }
        UserData.findById(id)
            ?.delete() ?: return@transaction false
        true
    }

    fun deleteTextureByID(id: Int): Boolean = transaction {
        TextureData.findById(id)
            ?.delete() ?: return@transaction false
        true
    }

    // ВНИМАНИЕ: также удалит все сессии на этой карте
    // Это делается для ненарушения ссылочной целостности
    fun deleteMapInfoByID(id: Int): Boolean = transaction {
        SessionData.find(SessionTable.mapID eq id)
            .forEach {
                SessionPlayerData.find(SessionPlayerTable.sessionID eq it.id)
                    .forEach { it.delete() }
                it.delete()
            }
        MapData.findById(id)
            ?.delete() ?: return@transaction false
        true
    }

    fun deleteSessionByID(id: Int): Boolean = transaction {
        SessionPlayerData.find(SessionPlayerTable.sessionID eq id)
            .forEach { it.delete() }
        SessionData.findById(id)
            ?.delete() ?: return@transaction false
        true
    }
}
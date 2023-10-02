package db

import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import sun.security.pkcs11.wrapper.CK_SESSION_INFO
import java.io.*

const val dbPath = "./data/roll_player"
const val dbTestPath = "./data/test_db"
const val mapsFolder = "./maps"
const val texturesFolder = "./textures"

object DBOperator {
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

    private fun fileExists(filePath: String) =
        (File(filePath)).isFile

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

    fun getAllTextures() = transaction {
        TextureData.all()
            .map { it.raw() }
    }

    fun createNewMap(fileName: String, mapName: String) = transaction {
        val mapPath = "$mapsFolder/${extractFileName(fileName)}.json"
        val mapFile = File(mapPath)
        if (!mapFile.createNewFile())
            throw FileAlreadyExistsException(mapFile, reason = "Map ${fileName} already exists")
        if (!MapData.find(MapTable.pathToJson eq mapPath).empty())
            throw FileAlreadyExistsException(mapFile, reason = "Map ${fileName} already recorded in the database")

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

    fun getAllUsers() = transaction { UserData.all().map { it.raw() } }
    fun getAllTextureInfos() = transaction { TextureData.all().map { it.raw() } }
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

    fun getPlayersOfSession(sessionId: Int) = transaction {
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

    fun addUser(userInfo: UserInfo) = transaction {
        if (userInfo.password.isBlank() || userInfo.login.isBlank())
            throw IllegalArgumentException("Cannot create user with blank login or password")

        if (!UserData.find(UserTable.login eq userInfo.login).empty())
            throw IllegalArgumentException("User with login `${userInfo.login}` already exists")

        UserData.new {
            login = userInfo.login
            password = userInfo.password
        }
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

    fun setSessionActive(sessionId: Int) = transaction {
        SessionData.findById(sessionId)
            .also { if (it == null) return@transaction false }
            ?.active = true
        true
    }

    fun setSessionInactive(sessionId: Int) = transaction {
        SessionData.findById(sessionId)
            .also { if (it == null) return@transaction false }
            ?.active = false
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

    fun removeNonExistingMaps() = transaction {
        MapData.all()
            .forEach {
                if (!fileExists(it.pathToJson))
                    it.delete()
            }
    }

    fun removeNonExistingTextures() = transaction {
        TextureData.all()
            .forEach {
                if (!fileExists(it.pathToFile))
                    it.delete()
            }
    }
}
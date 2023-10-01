package db

import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.*

const val dbPath = "./data/roll_player"
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

    fun connectOrCreate() {
        val exists = File("$dbPath.mv.db").isFile

        Database.connect("jdbc:h2:$dbPath")

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

    private fun fileExists(filePath: String) =
        (File(filePath)).isFile

    private fun deleteFileIfExists(filePath: String) =
        (File(filePath)).let { file ->
            if (file.exists())
                file.delete()
        }

    fun deleteDatabase() {
        deleteFileIfExists("$dbPath.mv.db")
        deleteFileIfExists("$dbPath.trace.db")
    }

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

    fun getUserByID(id: Int) = transaction { UserData[id].raw() }
    fun getTextureByID(id: Int) = transaction { TextureData[id].raw() }
    fun getMapByID(id: Int) = transaction { MapData[id].raw() }
    fun getSessionByID(id: Int) = transaction { SessionData[id].raw() }

    fun getPlayersOfSession(sessionId: Int) = transaction {
        SessionData[sessionId].players.map { it.raw() }
    }

    fun getPlayersGameStateOfSession(sessionId: Int) = transaction {
        SessionPlayerData.find(SessionPlayerTable.sessionID eq sessionId)
            .map { it.raw() }
    }

    fun getSessionsOfUser(userId: Int) = transaction {
        UserData[userId].sessions.map { it.raw() }
    }

    fun addUser(userInfo: UserInfo) = transaction {
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
            map = MapData[sessionInfo.mapID]
            active = sessionInfo.active
            started = sessionInfo.started
        }
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
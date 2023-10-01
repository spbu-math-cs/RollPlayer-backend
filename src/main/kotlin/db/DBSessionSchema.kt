package db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object SessionTable: IntIdTable("session", "session_id") {
    val mapID = reference("map_id", MapTable)
    val active = bool("active")
    val started = timestamp("started").nullable()
}

class SessionData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SessionData>(SessionTable)

    var map by MapData referencedOn SessionTable.mapID
    var active by SessionTable.active
    var started by SessionTable.started

    var players by UserData via SessionPlayerTable

    fun raw() = SessionInfo(map.id.value, active, started, id.value)
}

data class SessionInfo(val mapID: Int, val active: Boolean, val started: Instant?, val id: Int = -1)

object SessionPlayerTable: IntIdTable("session_player", "recording_id") {
    val sessionID = reference("session_id", SessionTable)
    val playerID = reference("player_id", UserTable)
    val xPos = integer("x_pos")
    val yPos = integer("y_pos")
}

class SessionPlayerData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SessionPlayerData>(SessionPlayerTable)

    var session by SessionData referencedOn SessionPlayerTable.sessionID
    var player by UserData referencedOn SessionPlayerTable.playerID

    var xPos by SessionPlayerTable.xPos
    var yPos by SessionPlayerTable.yPos
    
    fun raw() = SessionPlayerInfo(player.raw(), xPos, yPos)
}

data class SessionPlayerInfo(val player: UserInfo, val xPos: Int, val yPos: Int)
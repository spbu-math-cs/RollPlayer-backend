package db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object SessionTable: LongIdTable("session", "session_id") {
    val mapID = reference("map_id", MapTable)
    val started = timestamp("started")
}

class SessionData(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<SessionData>(SessionTable)

    var map by MapData referencedOn SessionTable.mapID
    var started by SessionTable.started

    fun raw() = Session(map.raw(), started)
}

data class Session(val map: Map, val started: Instant)

object SessionPlayerTable: LongIdTable("session_player", "recording_id") {
    val sessionID = reference("session_id", SessionTable)
    val playerID = reference("player_id", UserTable)
    val xPos = integer("x_pos")
    val yPos = integer("y_pos")
}

class SessionPlayerData(id: EntityID<Long>): LongEntity(id) {
    var session by SessionData referencedOn SessionPlayerTable.sessionID
    var player by UserData referencedOn SessionPlayerTable.playerID

    var xPos by SessionPlayerTable.xPos
    var yPos by SessionPlayerTable.yPos
}
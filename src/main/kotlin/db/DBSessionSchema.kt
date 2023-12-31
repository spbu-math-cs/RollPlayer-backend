package db

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object SessionTable: IntIdTable("session", "session_id") {
    val mapID = reference("map_id", MapTable,
        onDelete = ReferenceOption.CASCADE)
    val active = bool("active")
    val started = timestamp("started")
    val prevCharacterId = integer("prev_character_with_action")
}

class SessionData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SessionData>(SessionTable)

    var map by MapData referencedOn SessionTable.mapID
    var active by SessionTable.active
    var started by SessionTable.started
    var prevCharacterId by SessionTable.prevCharacterId

    val characters by CharacterData referrersOn CharacterTable.sessionID
    var users by UserData via CharacterTable

    fun raw() = SessionInfo(
        id.value.toUInt(),
        map.id.value.toUInt(),
        active,
        started.toKotlinInstant(),
        prevCharacterId
    )
}

@Serializable
data class SessionInfo(
    val id: UInt,
    val mapID: UInt,
    val active: Boolean,
    val started: Instant,
    val prevCharacterId: Int
)

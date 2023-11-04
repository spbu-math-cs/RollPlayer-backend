package db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object CharacterTable: IntIdTable("character", "character_id") {
    val sessionID = reference("session_id", SessionTable)
    val userID = reference("user_id", UserTable)
    val name = varchar("name", indentifierLength)
    val row = integer("x_pos")
    val col = integer("y_pos")
}

class CharacterData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<CharacterData>(CharacterTable)

    var session by SessionData referencedOn CharacterTable.sessionID
    var user by UserData referencedOn CharacterTable.userID

    var name by CharacterTable.name
    var row by CharacterTable.row
    var col by CharacterTable.col

    fun raw() = CharacterInfo(
        id.value.toUInt(),
        user.id.value.toUInt(),
        session.id.value.toUInt(),
        name, row, col)
}

@Serializable
data class CharacterInfo(
    val id: UInt,
    val userId: UInt,
    val sessionId: UInt,
    val name: String,
    val row: Int,
    val col: Int)
package db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object CharacterTable: IntIdTable("character", "character_id") {
    val sessionID = reference("session_id", SessionTable,
        onDelete = ReferenceOption.CASCADE) // теперь БД не будет падать при попытке удалить сессию,
    val userID = reference("user_id", UserTable, // не удалив персонажей сначала
        onDelete = ReferenceOption.CASCADE) // и это позволит избежать дублирования кода в DBOperator
    val name = varchar("name", identifierLength)
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

    val properties by PropertyData referrersOn PropertyTable.characterID

    fun raw() = CharacterInfo(
        id.value.toUInt(),
        user.id.value.toUInt(),
        session.id.value.toUInt(),
        name, row, col,
        properties.associateBy({ it.name }) { it.value })
}

@Serializable
data class CharacterInfo(
    val id: UInt,
    val userId: UInt,
    val sessionId: UInt,
    val name: String,
    val row: Int,
    val col: Int,
    val properties: kotlin.collections.Map<String, Int>)
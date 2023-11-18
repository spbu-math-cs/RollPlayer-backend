package db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object PropertyTable: IntIdTable("property", "property_id") {
    val name = varchar("name", identifierLength)
    val characterID = reference("character_id", CharacterTable)
    val value = integer("value")
}

class PropertyData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PropertyData>(PropertyTable)

    var name by PropertyTable.name
    var characterID by PropertyTable.characterID
    var value by PropertyTable.value
}

@Serializable
data class PropertyInfo(val id: UInt, val characterId: UInt, val name: String, val value: Int)
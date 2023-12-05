package db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object PropertyNameTable: IntIdTable("property_name", "property_name_id") {
    val name = varchar("name", identifierLength).uniqueIndex()
}

class PropertyNameData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PropertyNameData>(PropertyNameTable)

    var name by PropertyNameTable.name
}

object PropertyTable: IntIdTable("property", "property_id") {
    val nameID = reference("name_id", PropertyNameTable)
    val characterID = reference("character_id", CharacterTable,
        onDelete = ReferenceOption.CASCADE)
    val value = integer("value")

    init {
        uniqueIndex(characterID, nameID)
    }
}

class PropertyData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PropertyData>(PropertyTable)

    var nameData by PropertyNameData referencedOn PropertyTable.nameID
    var character by CharacterData referencedOn PropertyTable.characterID
    var value by PropertyTable.value
}
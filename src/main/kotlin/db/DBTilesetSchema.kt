package db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object TilesetTable: IntIdTable("tileset", "tileset_id") {
    val pathToJson = varchar("path_to_json", pathLength).uniqueIndex()
}

class TilesetData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<TilesetData>(TilesetTable)

    var pathToJson by TilesetTable.pathToJson

    fun raw(): TilesetInfo = TilesetInfo(id.value.toUInt(), pathToJson)
}

data class TilesetInfo(val id: UInt, val pathToJson: String)

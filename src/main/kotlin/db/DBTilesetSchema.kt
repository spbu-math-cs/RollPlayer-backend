package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object TilesetTable: IntIdTable("tileset", "tileset_id") {
    val pathToJson = varchar("path_to_json", pathLength).uniqueIndex()
}

class TilesetData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<TilesetData>(TilesetTable)

    var pathToJson by TilesetTable.pathToJson

    fun raw(): TilesetInfo = TilesetInfo(id.value.toUInt(), pathToJson)
}

data class TilesetInfo(val id: UInt, val pathToJson: String) {
    // fun load(): Tileset = Tileset(pathToJson)
}

/*
class Tileset(pathToJson: String) {
    init {
        TODO(): удалить
    }
}
*/
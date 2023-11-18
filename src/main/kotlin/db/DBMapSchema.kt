package db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object MapTable: IntIdTable("map", "map_id") {
    val pathToJson = varchar("path_to_json", pathLength).uniqueIndex()
}

class MapData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<MapData>(MapTable)

    var pathToJson by MapTable.pathToJson

    fun raw(): MapInfo = MapInfo(id.value.toUInt(), pathToJson)
}

@Serializable
data class MapInfo(val id: UInt, val pathToJson: String) {
    // fun load(): Map = Map(pathToJson)
}

/*
class Map(pathToJson: String) {
    init {
        TODO: мне кажется, это нужно удалить
    }
}
*/
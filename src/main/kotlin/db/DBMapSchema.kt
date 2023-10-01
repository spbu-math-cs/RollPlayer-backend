package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object MapTable: IntIdTable("map", "map_id") {
    val pathToJson = varchar("path_to_json", 1024).uniqueIndex()
}

class MapData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<MapData>(MapTable)

    var pathToJson by MapTable.pathToJson

    fun raw(): MapInfo = MapInfo(pathToJson, id.value)
}

data class MapInfo(val pathToJson: String, val id: Int = -1) {
    fun load(): Map = Map(pathToJson)
}

class Map(pathToJson: String) {
    init {
        TODO()
    }
}
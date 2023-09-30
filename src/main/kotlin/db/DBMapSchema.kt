package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object MapTable: IntIdTable("map", "map_id") {
    val path_to_json = varchar("path_to_json", 1024)
}

class MapData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<MapData>(MapTable)

    var pathToJson by MapTable.path_to_json

    fun raw(): Map = Map(pathToJson)
}

class Map(pathToJson: String) {
    init {
        TODO()
    }
}
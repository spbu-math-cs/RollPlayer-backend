package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object PictureTable: IntIdTable("avatar", "avatar_id") {
    val pathToFile = varchar("path_to_json", pathLength).uniqueIndex()
}

class PictureData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PictureData>(PictureTable)

    var pathToFile by PictureTable.pathToFile

    fun raw(): PictureInfo = PictureInfo(id.value.toUInt(), pathToFile)
}

data class PictureInfo(val id: UInt, val pathToFile: String)

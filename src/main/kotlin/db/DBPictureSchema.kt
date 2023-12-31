package db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object PictureTable: IntIdTable("picture", "picture_id") {
    val pathToFile = varchar("path_to_file", pathLength).uniqueIndex()
}

class PictureData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PictureData>(PictureTable)

    var pathToFile by PictureTable.pathToFile

    fun raw(): PictureInfo = PictureInfo(id.value.toUInt(), pathToFile)
}

data class PictureInfo(val id: UInt, val pathToFile: String)

package db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object TextureTable: IntIdTable("texture", "texture_id") {
    val pathToFile = varchar("path_to_file", pathLength).uniqueIndex()
}

class TextureData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<TextureData>(TextureTable)

    var pathToFile by TextureTable.pathToFile

    fun raw(): TextureInfo = TextureInfo(id.value.toUInt(), pathToFile)
}

@Serializable
data class TextureInfo(val id: UInt, val pathToFile: String)

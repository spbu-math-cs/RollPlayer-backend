package db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

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

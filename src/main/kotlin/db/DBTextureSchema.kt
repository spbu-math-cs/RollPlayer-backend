package db

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

data class TextureInfo(val id: UInt, val pathToFile: String) {
    fun load(): Texture = Texture(pathToFile)
}

class Texture(pathToFile: String) {
    init {
        TODO()
    }
}
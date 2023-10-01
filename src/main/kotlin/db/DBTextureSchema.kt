package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object TextureTable: IntIdTable("texture", "texture_id") {
    val pathToFile = varchar("path_to_file", 1024).uniqueIndex()
}

class TextureData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<TextureData>(TextureTable)

    var pathToFile by TextureTable.pathToFile

    fun raw(): TextureInfo = TextureInfo(pathToFile, id.value)
}

data class TextureInfo(val pathToFile: String, val id: Int = -1) {
    fun load(): Texture = Texture(pathToFile)
}

class Texture(pathToFile: String) {
    init {
        TODO()
    }
}
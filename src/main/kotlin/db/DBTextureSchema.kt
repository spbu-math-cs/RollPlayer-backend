package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object TextureTable: IntIdTable("texture", "texture_id") {
    val pathToFile = varchar("path_to_file", 1024)
}

class TextureData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<TextureData>(TextureTable)

    var pathToFile by TextureTable.pathToFile

    fun raw(): Texture = Texture(id.value, pathToFile)
}

data class Texture(val id: Int, val pathToFile: String)
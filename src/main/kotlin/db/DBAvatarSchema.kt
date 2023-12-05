package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object AvatarTable: IntIdTable("avatar", "avatar_id") {
    val pathToFile = varchar("path_to_json", pathLength).uniqueIndex()
}

class AvatarData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<AvatarData>(AvatarTable)

    var pathToFile by AvatarTable.pathToFile

    fun raw(): AvatarInfo = AvatarInfo(id.value.toUInt(), pathToFile)
}

data class AvatarInfo(val id: UInt, val pathToFile: String)

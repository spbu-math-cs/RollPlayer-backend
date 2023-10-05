package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

const val indentifierLength = 255
const val pathLength = 255

object UserTable: IntIdTable("user", "user_id") {
    val login = varchar("login", indentifierLength).uniqueIndex()
    val password = varchar("password", indentifierLength)
}

class UserData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserData>(UserTable)

    var login by UserTable.login
    var password by UserTable.password

    var sessions by SessionData via SessionPlayerTable

    fun raw(): UserInfo = UserInfo(login, password, id.value)
}

data class UserInfo(val login: String, val password: String, val id: Int = -1)
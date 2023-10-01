package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object UserTable: IntIdTable("user", "user_id") {
    val login = varchar("login", 255).uniqueIndex()
    val password = varchar("password", 255)
}

class UserData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserData>(UserTable)

    var login by UserTable.login
    var password by UserTable.password

    var sessions by SessionData via SessionPlayerTable

    fun raw(): UserInfo = UserInfo(login, password, id.value)
}

data class UserInfo(val login: String, val password: String, val id: Int = -1)
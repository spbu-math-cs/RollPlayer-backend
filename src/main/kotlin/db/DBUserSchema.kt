package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

object UserTable: IntIdTable("user", "user_id") {
    val login = varchar("login", 255)
    val password = varchar("password", 255)
}

class UserData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserData>(UserTable)

    var login by UserTable.login
    var password by UserTable.password

    fun raw(): User = User(id.value, login, password)
}

data class User(val id: Int, val login: String, val password: String)
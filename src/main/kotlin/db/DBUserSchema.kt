package db

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*

const val indentifierLength = 255
const val pathLength = 255

object UserTable: IntIdTable("user", "user_id") {
    val login = varchar("login", indentifierLength).uniqueIndex()
    val passwordHash = integer("password_hash")
    val pswHashInitial = integer("psw_hash_initial")
    val pswHashFactor = integer("psw_hash_factor")
}

class UserData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserData>(UserTable)

    var login by UserTable.login
    var passwordHash by UserTable.passwordHash
    var pswHashInitial by UserTable.pswHashInitial
    var pswHashFactor by UserTable.pswHashFactor

    var sessions by SessionData via SessionPlayerTable

    fun raw(): UserInfo = UserInfo(login, passwordHash, id.value)
}

data class UserInfo(val login: String, val passwordHash: Int, val id: Int = -1)
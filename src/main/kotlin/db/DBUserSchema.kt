package db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

const val identifierLength = 255
const val pathLength = 255

object UserTable: IntIdTable("user", "user_id") {
    val login = varchar("login", identifierLength).uniqueIndex()
    val email = varchar("email", identifierLength).uniqueIndex()
    val avatarID = reference("avatar_id", PictureTable).nullable()
    val passwordHash = integer("password_hash")
    val pswHashInitial = integer("psw_hash_initial")
    val pswHashFactor = integer("psw_hash_factor")
}

class UserData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserData>(UserTable)

    var login by UserTable.login
    var email by UserTable.email
    var passwordHash by UserTable.passwordHash
    var pswHashInitial by UserTable.pswHashInitial
    var pswHashFactor by UserTable.pswHashFactor

    val characters by CharacterData referrersOn CharacterTable.userID
    var sessions by SessionData via CharacterTable
    var avatar by PictureData optionalReferencedOn UserTable.avatarID

    fun raw(): UserInfo = UserInfo(
        id.value.toUInt(),
        login,
        email,
        passwordHash,
        avatar?.id?.value?.toUInt()
    )
}

@Serializable
data class UserInfo(
    val id: UInt,
    val login: String,
    val email: String,
    val passwordHash: Int,
    val avatarID: UInt?
)

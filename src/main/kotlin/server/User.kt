package server

data class User(
    val id: UInt,
    val email: String,
    val login: String,
    val password: Int
)
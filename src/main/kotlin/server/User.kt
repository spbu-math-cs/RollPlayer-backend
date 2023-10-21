package server

data class User(
    val id: Int,
    val email: String,
    val login: String,
    val password: String,
    val photo: String
)
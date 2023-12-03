package server.utils

class AttackException(attackType: String, message:String): Exception(message) {
    val attackType by lazy { attackType }
}

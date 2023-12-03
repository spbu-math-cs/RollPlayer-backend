package server.utils

enum class AttackFailReason(val str: String) {
    BigDist("big_dist"),
    LowMana("low_mana")
}

class AttackException(attackType: String, reason: AttackFailReason, message:String): Exception(message) {
    val attackType by lazy { attackType }
    val reason by lazy { reason }
}

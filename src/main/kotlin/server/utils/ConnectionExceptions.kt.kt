package server.utils

enum class MoveFailReason(val str: String) {
    BigDist("big_dist"),
    TileObstacle("tile_obstacle")
}

class MoveException(reason: MoveFailReason, message:String): Exception(message) {
    val reason by lazy { reason }
}

enum class AttackFailReason(val str: String) {
    BigDist("big_dist"),
    LowMana("low_mana")
}

class AttackException(attackType: String, reason: AttackFailReason, message:String): Exception(message) {
    val attackType by lazy { attackType }
    val reason by lazy { reason }
}

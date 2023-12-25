package server.utils

enum class CreationFailReason(val str: String) {
    TileObstacle("tile_obstacle")
}

class CreationException(reason: CreationFailReason, message:String): Exception(message) {
    val reason by lazy { reason }
}

enum class ActionFailReason(val str: String) {
    NotYourTurn("not_your_turn"),
    IsDefeated("is_defeated")
}

class ActionException(reason: ActionFailReason, message:String): Exception(message) {
    val reason by lazy { reason }
}

enum class MoveFailReason(val str: String) {
    BigDist("big_dist"),
    TileObstacle("tile_obstacle")
}

class MoveException(reason: MoveFailReason, message:String): Exception(message) {
    val reason by lazy { reason }
}

enum class AttackFailReason(val str: String) {
    BigDist("big_dist"),
    LowMana("low_mana"),
    OpponentIsDefeated("opponent_is_defeated")
}

class AttackException(attackType: String, reason: AttackFailReason, message:String): Exception(message) {
    val attackType by lazy { attackType }
    val reason by lazy { reason }
}

enum class ReviveFailReason(val str: String) {
    NotYourTurn("not_your_turn"),
    IsNotDefeated("is_not_defeated")
}

class ReviveException(reason: ReviveFailReason, message:String): Exception(message) {
    val reason by lazy { reason }
}

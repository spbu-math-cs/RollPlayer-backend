package server

import kotlinx.serialization.Serializable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

import org.json.JSONObject

@Serializable
data class PlayerProperties(
    var name: String,
    var x: Long,
    var y: Long
) {
    constructor(id: Int) : this("player${id}", 0, 0)
}

@Serializable
data class Player(val id: Int, var properties: PlayerProperties) {
    constructor(id: Int) : this(id, PlayerProperties(id))
}

fun updateProperties(old: PlayerProperties, new: JSONObject): PlayerProperties {
    PlayerProperties::class.memberProperties.forEach {
        if (it is KMutableProperty<*> && new.has(it.name)) {
            it.setter.call(old, new.get(it.name))
        }
    }
    return old
}

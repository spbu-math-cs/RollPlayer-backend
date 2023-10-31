package server

import kotlinx.serialization.Serializable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

import org.json.JSONObject

@Serializable
data class Character(
    val id: UInt,
    val userId: UInt,
    val sessionId: UInt,
    val name: String,
    var row: Int,
    var col: Int,
)

//fun updateProperties(old: PlayerProperties, new: JSONObject): PlayerProperties {
//    PlayerProperties::class.memberProperties.forEach {
//        if (it is KMutableProperty<*> && new.has(it.name)) {
//            it.setter.call(old, new.get(it.name))
//        }
//    }
//    return old
//}

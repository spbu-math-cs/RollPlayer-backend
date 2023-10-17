package server

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun createSimpleMap(): String {
    val map = List(64) { mapOf("id" to "0") }
    return Json.encodeToString(map)
}

package db

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlin.collections.Map

val propertyDisplayedNameMap = mapOf(
    "MAX_HP" to "Max health",
    "MAX_MP" to "Max mana",
    "CURR_HP" to "Current health",
    "CURR_MP" to "Current mana",
    "MELEE_AT_DMG" to "Melee attack damage",
    "RANGED_AT_DMG" to "Ranged attack damage",
    "MAGIC_AT_DMG" to "Magic attack damage",
    "MAGIC_AT_COST" to "Magic attack cost",
    "RANGED_AT_DIST" to "Ranged attack distance",
    "MAGIC_AT_DIST" to "Magic attack distance",
    "INIT" to "Initiative",
    "SPEED" to "Speed"
)

object PropertiesJsonArraySerializer:
    JsonTransformingSerializer<Map<String, Int>>(MapSerializer(String.serializer(), Int.serializer())) {
    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) {
            throw Exception("Incorrect element for PropertiesJsonArraySerializer")
        }
        return JsonArray(element.map { JsonObject(mapOf(
            "name" to JsonPrimitive(propertyDisplayedNameMap[it.key] ?: it.key),
            "value" to JsonPrimitive(it.value.toString().toInt())
        )) })
    }
}
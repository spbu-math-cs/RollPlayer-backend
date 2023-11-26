package db

import java.io.File
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.json.JSONObject

object MapTable: IntIdTable("map", "map_id") {
    val pathToJson = varchar("path_to_json", pathLength).uniqueIndex()
}

class MapData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<MapData>(MapTable)

    var pathToJson by MapTable.pathToJson

    fun raw(): MapInfo = MapInfo(id.value.toUInt(), pathToJson)
}

@Serializable
data class MapInfo(val id: UInt, val pathToJson: String) {
    fun load(): Map = Map(pathToJson)
}

class Map(pathToJson: String) {
    companion object {
        fun isObstacleLayer(layer: JSONObject): Boolean {
            if (!layer.has("properties")) {
                return false
            }
            layer.getJSONArray("properties").forEach { prop ->
                if (prop is JSONObject && prop.getString("name") == "Obstacle") {
                    return prop.getBoolean("value")
                }
            }
            return false
        }

        fun getLayerPassCost(layer: JSONObject): Int {
            if (!layer.has("properties")) {
                return 1
            }
            layer.getJSONArray("properties").forEach { prop ->
                if (prop is JSONObject && prop.getString("name") == "Pass cost") {
                    return prop.getInt("value")
                }
            }
            return 1
        }
    }

    private var obstacles: Array<BooleanArray> = emptyArray<BooleanArray>()
    private var passCosts: Array<IntArray> = emptyArray<IntArray>()

    init {
        val map = JSONObject(File(pathToJson).readText())
        val height = map.getInt("height")
        val width = map.getInt("width")
        obstacles = Array(height) { BooleanArray(width) { false } }
        passCosts = Array(height) { IntArray(width) { 0 } }
        map.getJSONArray("layers").forEach { layer ->
            layer as JSONObject
            val isObstacleLayer = isObstacleLayer(layer)
            val layerPassCost = if (isObstacleLayer) Int.MAX_VALUE else getLayerPassCost(layer)
            var tileIdx = 0
            layer.getJSONArray("data").forEach { tile ->
                tile as Int
                if (tile != 0) {
                    val row = tileIdx / width
                    val col = tileIdx % width
                    obstacles[row][col] = isObstacleLayer
                    passCosts[row][col] = layerPassCost
                }
                tileIdx++
            }
        }
    }

    fun isObstacleTile(row: Int, col: Int): Boolean = obstacles[row][col]

    fun getTilePassCost(row: Int, col: Int): Int = passCosts[row][col]
}
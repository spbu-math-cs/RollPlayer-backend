package db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.json.JSONObject
import java.io.File
import java.util.*

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
        data class Position(val row: Int, val col: Int)

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

        private fun getLayerIntProperty(layer: JSONObject, propName: String, defaultValue: Int = 0): Int {
            if (!layer.has("properties")) {
                return defaultValue
            }
            layer.getJSONArray("properties").forEach { prop ->
                if (prop is JSONObject && prop.getString("name") == propName) {
                    return prop.getInt("value")
                }
            }
            return defaultValue
        }

        fun getLayerPassCost(layer: JSONObject): Int {
            return getLayerIntProperty(layer, "Pass cost", 1)
        }

        fun getLayerHealthUpdate(layer: JSONObject): Int {
            return getLayerIntProperty(layer, "Restore HP") - getLayerIntProperty(layer, "Lose HP")
        }

        fun getLayerManaUpdate(layer: JSONObject): Int {
            return getLayerIntProperty(layer, "Restore MP") - getLayerIntProperty(layer, "Lose MP")
        }
    }

    private var height = 0
    private var width = 0
    private var obstacles: Array<BooleanArray> = emptyArray<BooleanArray>()
    private var passCosts: Array<IntArray> = emptyArray<IntArray>()
    private var healthUpdate: Array<IntArray> = emptyArray<IntArray>()
    private var manaUpdate: Array<IntArray> = emptyArray<IntArray>()

    init {
        val map = JSONObject(File(pathToJson).readText())
        height = map.getInt("height")
        width = map.getInt("width")
        obstacles = Array(height) { BooleanArray(width) { false } }
        passCosts = Array(height) { IntArray(width) { 0 } }
        healthUpdate = Array(height) { IntArray(width) { 0 } }
        manaUpdate = Array(height) { IntArray(width) { 0 } }
        map.getJSONArray("layers").forEach { layer ->
            layer as JSONObject
            val isObstacleLayer = isObstacleLayer(layer)
            val layerPassCost = if (isObstacleLayer) Int.MAX_VALUE else getLayerPassCost(layer)
            val layerHealthUpdate = getLayerHealthUpdate(layer)
            val layerManaUpdate = getLayerManaUpdate(layer)
            var tileIdx = 0
            layer.getJSONArray("data").forEach { tile ->
                tile as Int
                if (tile != 0) {
                    val row = tileIdx / width
                    val col = tileIdx % width
                    obstacles[row][col] = isObstacleLayer
                    passCosts[row][col] = layerPassCost
                    healthUpdate[row][col] += layerHealthUpdate
                    manaUpdate[row][col] += layerManaUpdate
                }
                tileIdx++
            }
        }
    }

    fun isObstacleTile(pos: Position): Boolean = obstacles[pos.row][pos.col]

    fun getTilePassCost(pos: Position): Int = passCosts[pos.row][pos.col]

    fun getTileHealthUpdate(pos: Position): Int {
        val update = healthUpdate[pos.row][pos.col]
        healthUpdate[pos.row][pos.col] = 0
        return update
    }

    fun getTileManaUpdate(pos: Position): Int {
        val update = manaUpdate[pos.row][pos.col]
        manaUpdate[pos.row][pos.col] = 0
        return update
    }

    private fun getNeighbors(pos: Position): List<Position> {
        val neighbors: MutableList<Position> = mutableListOf()
        for (deltaRow in -1..1) {
            for (deltaCol in -1..1) {
                val neighborPos = Position(pos.row + deltaRow, pos.col + deltaCol)
                if (neighborPos.row in 0 until height &&
                    neighborPos.col in 0 until width &&
                    !isObstacleTile(neighborPos))
                {
                    neighbors.add(neighborPos)
                }
            }
        }
        return neighbors
    }

    fun checkDistance(start: Position, finish: Position, distance: Int): Boolean {
        val dist = Array(height) { IntArray(width) { Int.MAX_VALUE } }
        dist[start.row][start.col] = 0
        val compareByDistance: Comparator<Pair<Int, Position>> = compareBy { it.first }
        val queue: PriorityQueue<Pair<Int, Position>> = PriorityQueue<Pair<Int, Position>>(compareByDistance)
        queue.add(Pair(0, start))
        while (!queue.isEmpty()) {
            val pos: Position = queue.remove().second
            if (dist[pos.row][pos.col] > distance) {
                return false
            }
            if (pos == finish) {
                return true
            }
            for (neighbor in getNeighbors(pos)) {
                if (dist[pos.row][pos.col] + passCosts[neighbor.row][neighbor.col] < dist[neighbor.row][neighbor.col]) {
                    dist[neighbor.row][neighbor.col] = dist[pos.row][pos.col] + passCosts[neighbor.row][neighbor.col]
                    queue.add(Pair(dist[neighbor.row][neighbor.col], neighbor))
                }
            }
        }
        return false
    }
}
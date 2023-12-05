package db

import java.io.File
import java.util.PriorityQueue
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

    private var height = 0
    private var width = 0
    private var obstacles: Array<BooleanArray> = emptyArray<BooleanArray>()
    private var passCosts: Array<IntArray> = emptyArray<IntArray>()

    init {
        val map = JSONObject(File(pathToJson).readText())
        height = map.getInt("height")
        width = map.getInt("width")
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

    fun isObstacleTile(pos: Position): Boolean = obstacles[pos.row][pos.col]

    fun getTilePassCost(pos: Position): Int = passCosts[pos.row][pos.col]

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
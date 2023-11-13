package server

import db.DBOperator
import db.SessionInfo
import io.ktor.websocket.*
import kotlinx.datetime.Instant
import org.json.JSONObject
import kotlin.collections.LinkedHashSet
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class ActiveSessionData(
    val sessionId: UInt,
    val mapId: UInt,
    val started: Instant,
    var moveProperties: MoveProperties = MoveProperties(),
    val connections: MutableList<Connection> = Collections.synchronizedList(mutableListOf()),
    val characters: MutableSet<UInt> = Collections.synchronizedSet(LinkedHashSet())
) {
    data class MoveProperties(
        var whoCanMove: AtomicInteger = AtomicInteger(0)  // userId
    )

    constructor(sessionInfo: SessionInfo): this(
        sessionInfo.id,
        sessionInfo.mapID,
        sessionInfo.started,
        MoveProperties(AtomicInteger(sessionInfo.whoCanMove))
    )

    fun toJson(): String {
        val json = JSONObject()
        json.put("sessionId", sessionId)
        json.put("mapId", mapId)
        json.put("started", started)
        return json.toString()
    }

    fun toSessionInfo(): SessionInfo {
        return SessionInfo(
            sessionId,
            mapId,
            true,
            started,
            moveProperties.whoCanMove.get()
        )
    }

    suspend fun updateMoveProperties() {
//        val users = DBOperator.getAllUsersInSession(sessionId)
//
//        val cur = connections.indexOfFirst { it.id == moveProperties.whoCanMove.get() }
//
//        val messageStatus = JSONObject()
//        messageStatus.put("type", "character:status")
//        messageStatus.put("can_move", false)
//        connections[cur].connection.send(messageStatus.toString())
//        val next = if (cur < connections.size - 1) cur + 1 else 0
//        moveProperties.whoCanMove = AtomicInteger(connections[next].id)
//        messageStatus.put("can_move", true)
//        connections[next].connection.send(messageStatus.toString())
    }

    fun validateMove(userId: Int) {
        if (userId != moveProperties.whoCanMove.get())
            throw Exception("Can not move now!")
    }
}

package server

import db.SessionInfo
import io.ktor.websocket.*
import kotlinx.datetime.Instant
import org.json.JSONObject
import kotlin.collections.LinkedHashSet
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

data class MoveProperties(
    var whoCanMove: AtomicInteger = AtomicInteger(0)
)

class ActiveSessionData(
    val sessionId: UInt,
    val mapId: UInt,
    val started: Instant,
    val connections: MutableList<Connection> = Collections.synchronizedList(mutableListOf()),
    val characters: MutableSet<UInt> = Collections.synchronizedSet(LinkedHashSet()),
    var moveProperties: MoveProperties = MoveProperties()
) {
    constructor(sessionInfo: SessionInfo): this(
        sessionInfo.id,
        sessionInfo.mapID,
        sessionInfo.started,
        characters = sessionInfo.characters.toMutableSet(),
        moveProperties = MoveProperties(AtomicInteger(sessionInfo.whoCanMove))
    )

    fun toSessionInfo(): SessionInfo {
        return SessionInfo(
            sessionId,
            mapId,
            true,
            started,
            moveProperties.whoCanMove.get(),
            characters
        )
    }

    // FIX: Здесь небезопасная ерунда написана, я знаю, но пока так.
    suspend fun updateMoveProperties() {
        val cur = connections.indexOfFirst { it.id == moveProperties.whoCanMove.get() }
        val messageStatus = JSONObject()
        messageStatus.put("type", "character:status")
        messageStatus.put("can_move", false)
        connections[cur].connection.send(messageStatus.toString())
        val next = if (cur < connections.size - 1) cur + 1 else 0
        moveProperties.whoCanMove = AtomicInteger(connections[next].id)
        messageStatus.put("can_move", true)
        connections[next].connection.send(messageStatus.toString())
    }

    fun validateMove(connectionId: Int) {
        if (connectionId != moveProperties.whoCanMove.get())
            throw Exception("Can not move now!")
    }
}

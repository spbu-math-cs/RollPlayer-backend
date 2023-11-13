package server

import db.*
import server.routing.*

import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.util.*
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

fun Application.module() {
    val port = environment.config.propertyOrNull("ktor.deployment.port")
        ?.getString()?.toIntOrNull() ?: 9999
    val host = environment.config.propertyOrNull("ktor.deployment.host")
        ?.getString() ?: "127.0.0.1"

    embeddedServer(Netty, port = port, host = host) {
        extracted()
    }.start(wait = true)
}

private fun Application.extracted() {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = Duration.ofSeconds(2)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val activeSessions = Collections.synchronizedMap<UInt, ActiveSessionData>(mutableMapOf())
    // val charactersByID = Collections.synchronizedMap<UInt, Character>(mutableMapOf())

    DBOperator.connectOrCreate(true)

    logger.info("Server is ready")
    routing {
        staticFiles("", File("static"))

        requestsUser()
        requestsMap()

        createSession()
        connection(activeSessions)
    }
}

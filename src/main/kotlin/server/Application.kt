package server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import db.DBOperator
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import server.routing.*
import java.io.File
import java.time.Duration
import java.util.*

val logger = KotlinLogging.logger {}

data class JWTParams(
    val secret: String,
    val issuer: String,
    val audience: String,
    val myRealm: String
)

fun Application.module() {
    val port = environment.config.propertyOrNull("ktor.deployment.port")
        ?.getString()?.toIntOrNull() ?: 9999
    val host = environment.config.propertyOrNull("ktor.deployment.host")
        ?.getString() ?: "0.0.0.0"

    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()

    embeddedServer(Netty, port = port, host = host) {
        extracted(JWTParams(secret, issuer, audience, myRealm))
    }.start(wait = true)
}

private fun Application.extracted(jwtParams: JWTParams) {
    install(io.ktor.server.plugins.cors.routing.CORS) {
        anyHost()
    }

    install(ContentNegotiation) {
        json()
    }

    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = Duration.ofSeconds(2)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val jwtVerifier = JWT
        .require(Algorithm.HMAC256(jwtParams.secret))
        .withAudience(jwtParams.audience)
        .withIssuer(jwtParams.issuer)
        .build()

    install(Authentication) {
        jwt ("auth-jwt") {
            realm = jwtParams.myRealm

            verifier(jwtVerifier)

            validate { credential ->
                if (credential.payload.getClaim("id").asString() != "" &&
                    credential.payload.getClaim("login").asString() != "" &&
                    (credential.expiresAt?.time?.minus(System.currentTimeMillis()) ?: 0) > 0
                ) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }

    val activeSessions = Collections.synchronizedMap<UInt, ActiveSessionData>(mutableMapOf())

    DBOperator.connectOrCreate(true)

    logger.info("Server is ready")
    routing {
        staticFiles("", File("static"))

        requestsUser(jwtParams)
        requestsMap()
        requestsPictures()

        gameSession()
        gameSessionConnection(activeSessions, jwtVerifier)
    }
}

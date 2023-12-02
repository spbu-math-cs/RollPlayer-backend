import db.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import server.ActiveSessionData
import server.Connection
import server.module
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntegrationTests {
    @Test
    fun `WEBSOCKET request to connect to session userId incorrect returns expected response()` {

        testApplication {
            routing {
                webSocket("/api/connect//") {
                    for (frame in incoming) {
                        assertEquals("null", (frame as Frame.Text).readText())
                        outgoing.send(frame)
                    }
                }
            }

            createClient {
                install(io.ktor.server.websocket.WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(
                        Json {
                            ignoreUnknownKeys = true
                        }
                    )
                }
            }.ws("/api/connect/{userId}/{sessionId}") {
                val req: String? = null
                sendSerialized(req)
                val received = receiveDeserialized<String>()
                assertNotNull(received)

            }
        }
    }
}
import io.ktor.http.*
import io.ktor.server.testing.*
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.websocket.*
import org.junit.Test
import server.module

class ServerTest {

    @Test
    fun testGetTextures() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/api/textures").apply {
                assertThat(response.content).isEqualTo(
                    """
                    [
                        {"id":"1","url":"/path/to/texture1"},
                        {"id":"2","url":"/path/to/texture2"}
                    ]
                    """.trimIndent()
                )
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun testGetTextureById() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/api/textures/1").apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun testWebSocketConnection() {
        withTestApplication({ module() }) {
            handleWebSocketConversation("/api/connect") { incoming, outgoing ->
                outgoing.send(Frame.Text("""{"property": "value"}"""))
                val response = (incoming.receive() as Frame.Text).readText()
                assertThat(response).isEqualTo("""{"property": "updatedValue"}""")
            }
        }
    }
}

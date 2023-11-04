import server.*

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import org.hamcrest.MatcherAssert.assertThat

import org.junit.Test

class ServerTest {
    @Test
    fun testGetTextures() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/api/textures").apply {
//                assertThat(response.content).isEqualTo(
//                    """
//                    [
//                        {"id":"1","url":"/path/to/texture1"},
//                        {"id":"2","url":"/path/to/texture2"}
//                    ]
//                    """.trimIndent()
//                )
//                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun testGetTextureById() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/api/textures/1").apply {
                //assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

}

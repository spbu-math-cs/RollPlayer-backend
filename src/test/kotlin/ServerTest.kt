import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import junit.framework.TestCase.*
import org.junit.Test
import server.module
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import db.DBOperator
import db.Texture
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.io.File

data class TextureInfo(val id: String, val filePath: String)
class RoutingTest {

//    @Test
//    fun testWebSocketConnection() {
//        withTestApplication({ module() }) {
//            handleWebSocketConversation("/api/connect/userId/sessionId", {}) { incoming, outgoing ->
//                // Simulate WebSocket connection and messages here
//
//                try {
//                    // Send a message to the server
//                    outgoing.send(Frame.Text("{'type': 'character:new', 'name': 'TestCharacter'}"))
//
//                    // Receive and validate the response from the server
//                    val frame = incoming.receive()
//                    assertTrue(frame is Frame.Text)
//                    val response = (frame as Frame.Text).readText()
//                    // Validate the response content here
//
//                    // You can send more messages and validate the responses as needed
//                } catch (e: Exception) {
//                    // Handle exceptions if necessary
//                } finally {
//                    // Close the WebSocket connection with a CloseReason wrapped in a Throwable
//                    // val closeReason = CloseReason(CloseReason.Codes.NORMAL, "Test finished")
//                    // outgoing.close(CloseReason(CloseReason.Codes.NORMAL, "Test finished"))
//                }
//            }
//        }
//    }



    @Test
    fun testGetTexturesApi() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/api/textures").apply {
                assertEquals(HttpStatusCode.OK, response.status())

                val expectedResponse = listOf(
                    mapOf("id" to "1", "filepath" to "path/to/texture1"),
                    mapOf("id" to "2", "filepath" to "path/to/texture2"),
                )
                val actualResponse = jacksonObjectMapper().readValue<List<Map<String, String>>>(response.content!!)

                assertEquals(expectedResponse, actualResponse)

            }
        }
    }


    @Test
    fun testCharacterRemoveEndpoint() {
        withTestApplication({ module() }) {
            val characterId = "123"
            handleRequest(HttpMethod.Delete, "/api/character/remove/$characterId") {
            }.apply {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }


    @Test
    fun testGetTextureByIdEndpoint() {
        mockkObject(DBOperator)
        every { DBOperator.getTextureByID(any()) } returns TextureInfo()

        withTestApplication({ module() }) {
            val textureId = "1" // Параметр id текстуры

            handleRequest(HttpMethod.Get, "/api/textures/$textureId").apply {
                // Проверяем, что статус код соответствует ожидаемому (200 OK)
                assertEquals(HttpStatusCode.OK, response.status())

                // Опционально: Проверяем другие аспекты ответа, если это необходимо
                // ...

                // Опционально: Проверяем логирование успешного запроса, если это необходимо
                // ...
            }
        }

        unmockkObject(DBOperator)
    }
}


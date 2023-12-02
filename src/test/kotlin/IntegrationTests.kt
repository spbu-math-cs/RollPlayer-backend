import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import server.module
import kotlin.test.assertEquals

class IntegrationTests {

    @Test
    fun `Integration test for session creation and character manipulation`() {
        withTestApplication({ module() }) {
            handleWebSocketConversation("/api/connect/1/1") { incoming, outgoing ->
                val createSessionRequest = """
                    {"action": "createSession", "userId": 1, "sessionId": 1}
                """.trimIndent()
                outgoing.send(Frame.Text(createSessionRequest))
                val createSessionResponse = incoming.receive() as Frame.Text
                val createSessionResponseBody = Json.decodeFromString<Map<String, String>>(createSessionResponse.readText())
                assertEquals("Session created successfully", createSessionResponseBody["message"])
            }

            handleWebSocketConversation("/api/connect/1/1") { incoming, outgoing ->
                val addCharacterRequest = """
                    {"action": "addCharacter", "userId": 1, "sessionId": 1, "characterId": 1}
                """.trimIndent()
                val addCharacterResponse = incoming.receive() as Frame.Text
                val addCharacterResponseBody = Json.decodeFromString<Map<String, String>>(addCharacterResponse.readText())
                assertEquals("Character added successfully", addCharacterResponseBody["message"])
            }

            handleWebSocketConversation("/api/connect/1/1") { incoming, outgoing ->
                val moveCharacterRequest = """
                    {"action": "moveCharacter", "userId": 1, "sessionId": 1, "characterId": 1, "newPosition": "newPos"}
                """.trimIndent()
                outgoing.send(Frame.Text(moveCharacterRequest))
                val moveCharacterResponse = incoming.receive() as Frame.Text
                val moveCharacterResponseBody = Json.decodeFromString<Map<String, String>>(moveCharacterResponse.readText())
                assertEquals("Character moved successfully", moveCharacterResponseBody["message"])
            }

            handleWebSocketConversation("/api/connect/1/1") { incoming, outgoing ->
                val removeCharacterRequest = """
                    {"action": "removeCharacter", "userId": 1, "sessionId": 1, "characterId": 1}
                """.trimIndent()
                outgoing.send(Frame.Text(removeCharacterRequest))
                val removeCharacterResponse = incoming.receive() as Frame.Text
                val removeCharacterResponseBody = Json.decodeFromString<Map<String, String>>(removeCharacterResponse.readText())
                assertEquals("Character removed successfully", removeCharacterResponseBody["message"])
            }

            handleWebSocketConversation("/api/connect/2/2") { incoming, outgoing ->
                val createSecondSessionRequest = """
                    {"action": "createSession", "userId": 2, "sessionId": 2}
                """.trimIndent()
                outgoing.send(Frame.Text(createSecondSessionRequest))
                val createSecondSessionResponse = incoming.receive() as Frame.Text
                val createSecondSessionResponseBody = Json.decodeFromString<Map<String, String>>(createSecondSessionResponse.readText())
                assertEquals("Session created successfully", createSecondSessionResponseBody["message"])
            }

            handleWebSocketConversation("/api/connect/2/2") { incoming, outgoing ->
                val getCharactersRequest = """
                    {"action": "getCharacters", "userId": 2, "sessionId": 2}
                """.trimIndent()
                outgoing.send(Frame.Text(getCharactersRequest))
                val getCharactersResponse = incoming.receive() as Frame.Text
                val getCharactersResponseBody = Json.decodeFromString<Map<String, Any>>(getCharactersResponse.readText())
                assertEquals("Characters retrieved successfully", getCharactersResponseBody["message"])
                val charactersList = getCharactersResponseBody["characters"] as List<Map<String, Any>>
                assertEquals(1, charactersList.size)
                assertEquals(1u, charactersList[0]["characterId"])
            }

            handleWebSocketConversation("/api/connect/2/2") { incoming, outgoing ->
                val moveCharacterRequest = """
                    {"action": "moveCharacter", "userId": 2, "sessionId": 2, "characterId": 1, "newPosition": "newPos"}
                """.trimIndent()
                outgoing.send(Frame.Text(moveCharacterRequest))
                val moveCharacterResponse = incoming.receive() as Frame.Text
                val moveCharacterResponseBody = Json.decodeFromString<Map<String, String>>(moveCharacterResponse.readText())
                assertEquals("It's not your turn to move", moveCharacterResponseBody["message"])
            }
        }
    }
}

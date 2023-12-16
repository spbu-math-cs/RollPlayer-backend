import db.*
import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import server.module

class WebSocketTest {
    @Test
    fun `test Websocket with incorrect sessionId`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                2u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals("Invalid sessionId: session does not exist", responseText)
        }
    }

    @Test
    fun `test Websocket with incorrect userId`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(2u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "Invalid userId: user does not exist",
                responseText
            )
        }
    }

    @Test
    fun `test Websocket with not UInt userId`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(2u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "Invalid userId: user does not exist",
                responseText
            )
        }
    }


    @Test
    fun `test Websocket with correct parameters`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"mapId\":\"1\",\"started\":\"2023-12-14T21:50:43.434307698Z\",\"sessionId\":\"1\"}",
                responseText
            )
        }
    }

    @Test
    fun `test Websocket with action type character_new`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:new\"}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"character\":{\"basicProperties\":{\"dexterity\":0,\"constitution\":0,\"strength\":0,\"charisma\":0,\"intelligence\":0,\"wisdom\":0},\"col\":0,\"isDefeated\":false,\"avatarId\":null,\"name\":\"Dovakin\",\"id\":1,\"sessionId\":1,\"row\":0,\"userId\":1,\"properties\":[{\"name\":\"Max health\",\"value\":100},{\"name\":\"Max mana\",\"value\":150},{\"name\":\"Current health\",\"value\":100},{\"name\":\"Current mana\",\"value\":150},{\"name\":\"Melee attack damage\",\"value\":30},{\"name\":\"Ranged attack damage\",\"value\":20},{\"name\":\"Magic attack damage\",\"value\":120},{\"name\":\"Magic attack cost\",\"value\":25},{\"name\":\"Ranged attack distance\",\"value\":16},{\"name\":\"Magic attack distance\",\"value\":16},{\"name\":\"Initiative\",\"value\":16},{\"name\":\"Speed\",\"value\":5}]},\"own\":true,\"type\":\"character:new\"}",
                responseText
            )
        }
    }

    @Test
    fun `test Websocket with action type character_remove without characterId`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:remove\"}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"type\":\"error\",\"message\":\"JSONObject[\\\"id\\\"] not found.\",\"on\":\"character:remove\"}",
                responseText
            )
        }
    }


    @Test
    fun `test Websocket with action type character_remove with characterId`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:remove\", \"id\": 1}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"type\": \"character:remove\", \"id\": 1}",
                responseText
            )
        }
    }


    @Test
    fun `test Websocket with action type character_move with incorrect characterId`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:move\", \"id\": 100}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"type\":\"error\",\"message\":\"Character with ID 100 does not exist\",\"on\":\"character:move\"}",
                responseText
            )
        }
    }

    @Test
    fun `test Websocket with action type character_move with correct parameters`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:move\", \"id\": 2, \"row\": 1, \"col\": 0}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"type\": \"character:move\", \"id\": 1, \"character\": {\"basicProperties\":{\"dexterity\":0,\"constitution\":0,\"strength\":0,\"charisma\":0,\"intelligence\":0,\"wisdom\":0},\"col\":0,\"isDefeated\":false,\"avatarId\":null,\"name\":\"Dovakin\",\"id\":1,\"sessionId\":1,\"row\":1,\"userId\":1,\"properties\":[{\"name\":\"Max health\",\"value\":100},{\"name\":\"Max mana\",\"value\":150},{\"name\":\"Current health\",\"value\":100},{\"name\":\"Current mana\",\"value\":150},{\"name\":\"Melee attack damage\",\"value\":30},{\"name\":\"Ranged attack damage\",\"value\":20},{\"name\":\"Magic attack damage\",\"value\":120},{\"name\":\"Magic attack cost\",\"value\":25},{\"name\":\"Ranged attack distance\",\"value\":16},{\"name\":\"Magic attack distance\",\"value\":16},{\"name\":\"Initiative\",\"value\":16},{\"name\":\"Speed\",\"value\":5}]}}",
                responseText
            )
        }
    }


    @Test
    fun `test Websocket with action type character_move to obstacle`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:move\", \"id\": 2, \"row\": 1, \"col\": 1}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"reason\":\"tile_obstacle\",\"type\":\"error\",\"message\":\"Can't move: target tile is obstacle\",\"on\":\"character:move\"}",
                responseText
            )
        }
    }


    @Test
    fun `test Websocket with action type character_move to so far tile`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(any()) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:move\", \"id\": 2, \"row\": 20, \"col\": 20}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"reason\":\"big_dist\",\"type\":\"error\",\"message\":\"Can't move: target tile is too far\",\"on\":\"character:move\"}",
                responseText
            )
        }
    }


    @Test
    fun `test Websocket with action type character_attack melee`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(1u) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getUserByID(2u) } returns UserInfo(2u, "testLogin2", "test2@email.ru", 1234567890, 1u)
            every { getCharacterByID(1u) } returns CharacterInfo(
                id = 1u,
                basicProperties = BasicProperties(
                    dexterity = 0,
                    constitution = 0,
                    charisma = 0,
                    intelligence = 0,
                    wisdom = 0
                ),
                col = 0,
                row = 0,
                isDefeated = false,
                avatarId = null,
                name = "Dovakin",
                userId = 1u,
                sessionId = 1u,
                properties = mapOf(
                    "MAX_HP" to 100,
                    "CURR_HP" to 100,
                    "MAX_MANA" to 150,
                    "CUR_MANA" to 150,
                    "RANGED_AT_DMG" to 20,
                    "MELEE_AT_DMG" to 30,
                    "MAGIC_AT_DMG" to 120,
                    "MAGIC_AT_COST" to 25,
                    "RANGED_AT_DIST" to 16,
                    "MAGIC_AT_DIST" to 16,
                    "SPEED" to 5
                )
            )
            every { getCharacterByID(2u) } returns CharacterInfo(
                id = 2u,
                basicProperties = BasicProperties(
                    dexterity = 0,
                    constitution = 0,
                    charisma = 0,
                    intelligence = 0,
                    wisdom = 0
                ),
                col = 0,
                row = 0,
                isDefeated = false,
                avatarId = null,
                name = "Dovakin",
                userId = 2u,
                sessionId = 1u,
                properties = mapOf(
                    "MAX_HP" to 100,
                    "CURR_HP" to 100,
                    "MAX_MANA" to 150,
                    "CUR_MANA" to 150,
                    "RANGED_AT_DMG" to 20,
                    "MELEE_AT_DMG" to 30,
                    "MAGIC_AT_DMG" to 120,
                    "MAGIC_AT_COST" to 25,
                    "RANGED_AT_DIST" to 16,
                    "MAGIC_AT_DIST" to 16,
                    "SPEED" to 5
                )
            )
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:attack\", \"id\": 1, \"attackType\": \"melee\", \"opponentId\": 2}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"can_do_action\":false,\"id\":1,\"type\":\"character:status\"}",
                responseText
            )
        }
    }


    @Test
    fun `test Websocket with action type character_attack magic`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(1u) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getUserByID(2u) } returns UserInfo(2u, "testLogin2", "test2@email.ru", 1234567890, 1u)
            every { getCharacterByID(1u) } returns CharacterInfo(
                id = 1u,
                basicProperties = BasicProperties(
                    dexterity = 0,
                    constitution = 0,
                    charisma = 0,
                    intelligence = 0,
                    wisdom = 0
                ),
                col = 0,
                row = 0,
                isDefeated = false,
                avatarId = null,
                name = "Dovakin",
                userId = 1u,
                sessionId = 1u,
                properties = mapOf(
                    "MAX_HP" to 100,
                    "CURR_HP" to 100,
                    "MAX_MANA" to 150,
                    "CUR_MANA" to 150,
                    "RANGED_AT_DMG" to 20,
                    "MELEE_AT_DMG" to 30,
                    "MAGIC_AT_DMG" to 120,
                    "MAGIC_AT_COST" to 25,
                    "RANGED_AT_DIST" to 16,
                    "MAGIC_AT_DIST" to 16,
                    "SPEED" to 5
                )
            )
            every { getCharacterByID(2u) } returns CharacterInfo(
                id = 2u,
                basicProperties = BasicProperties(
                    dexterity = 0,
                    constitution = 0,
                    charisma = 0,
                    intelligence = 0,
                    wisdom = 0
                ),
                col = 0,
                row = 0,
                isDefeated = false,
                avatarId = null,
                name = "Dovakin",
                userId = 2u,
                sessionId = 1u,
                properties = mapOf(
                    "MAX_HP" to 100,
                    "CURR_HP" to 100,
                    "MAX_MANA" to 150,
                    "CUR_MANA" to 150,
                    "RANGED_AT_DMG" to 20,
                    "MELEE_AT_DMG" to 30,
                    "MAGIC_AT_DMG" to 120,
                    "MAGIC_AT_COST" to 25,
                    "RANGED_AT_DIST" to 16,
                    "MAGIC_AT_DIST" to 16,
                    "SPEED" to 5
                )
            )
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:attack\", \"id\": 1, \"attackType\": \"magic\", \"opponentId\": 2}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"can_do_action\":false,\"id\":1,\"type\":\"character:status\"}",
                responseText
            )
        }
    }

    @Test
    fun `test Websocket with action type character_attack ranged`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(1u) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getUserByID(2u) } returns UserInfo(2u, "testLogin2", "test2@email.ru", 1234567890, 1u)
            every { getCharacterByID(1u) } returns CharacterInfo(
                id = 1u,
                basicProperties = BasicProperties(
                    dexterity = 0,
                    constitution = 0,
                    charisma = 0,
                    intelligence = 0,
                    wisdom = 0
                ),
                col = 0,
                row = 0,
                isDefeated = false,
                avatarId = null,
                name = "Dovakin",
                userId = 1u,
                sessionId = 1u,
                properties = mapOf(
                    "MAX_HP" to 100,
                    "CURR_HP" to 100,
                    "MAX_MANA" to 150,
                    "CUR_MANA" to 150,
                    "RANGED_AT_DMG" to 20,
                    "MELEE_AT_DMG" to 30,
                    "MAGIC_AT_DMG" to 120,
                    "MAGIC_AT_COST" to 25,
                    "RANGED_AT_DIST" to 16,
                    "MAGIC_AT_DIST" to 16,
                    "SPEED" to 5
                )
            )
            every { getCharacterByID(2u) } returns CharacterInfo(
                id = 2u,
                basicProperties = BasicProperties(
                    dexterity = 0,
                    constitution = 0,
                    charisma = 0,
                    intelligence = 0,
                    wisdom = 0
                ),
                col = 0,
                row = 0,
                isDefeated = false,
                avatarId = null,
                name = "Dovakin",
                userId = 2u,
                sessionId = 1u,
                properties = mapOf(
                    "MAX_HP" to 100,
                    "CURR_HP" to 100,
                    "MAX_MANA" to 150,
                    "CUR_MANA" to 150,
                    "RANGED_AT_DMG" to 20,
                    "MELEE_AT_DMG" to 30,
                    "MAGIC_AT_DMG" to 120,
                    "MAGIC_AT_COST" to 25,
                    "RANGED_AT_DIST" to 16,
                    "MAGIC_AT_DIST" to 16,
                    "SPEED" to 5
                )
            )
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:attack\", \"id\": 1, \"attackType\": \"ranged\", \"opponentId\": 2}"))
            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"can_do_action\":false,\"id\":1,\"type\":\"character:status\"}",
                responseText
            )
        }
    }

    @Test
    fun `test Websocket with action type character_attack dupliacte`() = testApplication {

        application {
            module()
        }

        mockk<DBOperator> {
            every { getUserByID(1u) } returns UserInfo(1u, "testLogin", "test@email.ru", 1234567890, 1u)
            every { getUserByID(2u) } returns UserInfo(2u, "testLogin2", "test2@email.ru", 1234567890, 1u)
            every { getCharacterByID(1u) } returns CharacterInfo(
                id = 1u,
                basicProperties = BasicProperties(
                    dexterity = 0,
                    constitution = 0,
                    charisma = 0,
                    intelligence = 0,
                    wisdom = 0
                ),
                col = 0,
                row = 0,
                isDefeated = false,
                avatarId = null,
                name = "Dovakin",
                userId = 1u,
                sessionId = 1u,
                properties = mapOf(
                    "MAX_HP" to 100,
                    "CURR_HP" to 100,
                    "MAX_MANA" to 150,
                    "CUR_MANA" to 150,
                    "RANGED_AT_DMG" to 20,
                    "MELEE_AT_DMG" to 30,
                    "MAGIC_AT_DMG" to 120,
                    "MAGIC_AT_COST" to 25,
                    "RANGED_AT_DIST" to 16,
                    "MAGIC_AT_DIST" to 16,
                    "SPEED" to 5
                )
            )
            every { getCharacterByID(2u) } returns CharacterInfo(
                id = 2u,
                basicProperties = BasicProperties(
                    dexterity = 0,
                    constitution = 0,
                    charisma = 0,
                    intelligence = 0,
                    wisdom = 0
                ),
                col = 0,
                row = 0,
                isDefeated = false,
                avatarId = null,
                name = "Dovakin",
                userId = 2u,
                sessionId = 1u,
                properties = mapOf(
                    "MAX_HP" to 100,
                    "CURR_HP" to 100,
                    "MAX_MANA" to 150,
                    "CUR_MANA" to 150,
                    "RANGED_AT_DMG" to 20,
                    "MELEE_AT_DMG" to 30,
                    "MAGIC_AT_DMG" to 120,
                    "MAGIC_AT_COST" to 25,
                    "RANGED_AT_DIST" to 16,
                    "MAGIC_AT_DIST" to 16,
                    "SPEED" to 5
                )
            )
            every { getSessionByID(any()) } returns SessionInfo(
                1u,
                1u,
                true,
                Instant.fromEpochSeconds(java.time.Instant.MIN.epochSecond),
                1
            )
        }

        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/api/connect/1/1") {

            send(Frame.Text("{\"type\": \"character:attack\", \"id\": 1, \"attackType\": \"ranged\", \"opponentId\": 2}"))

            send(Frame.Text("{\"type\": \"character:attack\", \"id\": 1, \"attackType\": \"ranged\", \"opponentId\": 2}"))

            val responseText = (incoming.receive() as Frame.Text).readText()
            assertEquals(
                "{\"reason\":\"not_your_turn\",\"type\":\"error\",\"message\":\"Can't do action: not your turn now\",\"on\":\"character:attack\"}",
                responseText
            )
        }
    }
}
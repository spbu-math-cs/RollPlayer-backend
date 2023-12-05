package db

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.collections.Map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object CharacterTable: IntIdTable("character", "character_id") {
    val sessionID = reference("session_id", SessionTable,
        onDelete = ReferenceOption.CASCADE)
    val userID = reference("user_id", UserTable,
        onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", identifierLength)
    val avatarID = reference("avatar_id", PictureTable).nullable()
    val row = integer("row")
    val col = integer("col")

    val strength = integer("strength")
    val dexterity = integer("dexterity")
    val constitution = integer("constitution")
    val intelligence = integer("intelligence")
    val wisdom = integer("wisdom")
    val charisma = integer("charisma")
}

class CharacterData(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<CharacterData>(CharacterTable)

    var session by SessionData referencedOn CharacterTable.sessionID
    var user by UserData referencedOn CharacterTable.userID

    var name by CharacterTable.name
    var row by CharacterTable.row
    var col by CharacterTable.col

    var avatar by PictureData optionalReferencedOn CharacterTable.avatarID
    val properties by PropertyData referrersOn PropertyTable.characterID

    var strength by CharacterTable.strength
    var dexterity by CharacterTable.dexterity
    var constitution by CharacterTable.constitution
    var intelligence by CharacterTable.intelligence
    var wisdom by CharacterTable.wisdom
    var charisma by CharacterTable.charisma

    fun getBasicProperties(): BasicProperties {
        return BasicProperties(strength, dexterity, constitution, intelligence, wisdom, charisma)
    }

    fun raw() = CharacterInfo(
        id.value.toUInt(),
        user.id.value.toUInt(),
        session.id.value.toUInt(),
        name,
        avatar?.id?.value?.toUInt(),
        row, col,
        getBasicProperties())
        // properties.associateBy({ it.nameData.name }) { it.value })
}

object PropertiesJsonArraySerializer:
    JsonTransformingSerializer<Map<String, Int>>(MapSerializer(String.serializer(), Int.serializer())) {
    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) {
            throw Exception("Incorrect element for PropertiesJsonArraySerializer")
        }
        return JsonArray(element.map { JsonObject(mapOf(
            "name" to JsonPrimitive(it.key),
            "value" to JsonPrimitive(it.value.toString().toInt())
        )) })
    }
}

@Serializable
data class BasicProperties @OptIn(ExperimentalSerializationApi::class) constructor(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val strength: Int = 0, // FIXME: значение по умолчанию 0 или нет??
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val dexterity: Int = 0,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val constitution: Int = 0,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val intelligence: Int = 0,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val wisdom: Int = 0,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val charisma: Int = 0
)

@Serializable
data class CharacterInfo(
    val id: UInt,
    val userId: UInt,
    val sessionId: UInt,
    val name: String,
    val avatarId: UInt?,
    val row: Int,
    val col: Int,
    val basicProperties: BasicProperties
    // @Serializable(PropertiesJsonArraySerializer::class) val properties: Map<String, Int>
)

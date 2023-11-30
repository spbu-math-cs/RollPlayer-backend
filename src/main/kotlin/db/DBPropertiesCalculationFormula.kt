package db

import kotlin.collections.Map

private fun constant(value: Int) = { _: BasicProperties -> value }
private fun linear(strCoeff: Int, dexCoeff: Int, conCoeff: Int, intCoeff: Int, wisCoeff: Int, chaCoeff: Int) = {
    bProps: BasicProperties -> bProps.strength * strCoeff +
        bProps.dexterity * dexCoeff +
        bProps.constitution * conCoeff +
        bProps.intelligence * intCoeff +
        bProps.wisdom * wisCoeff +
        bProps.charisma * chaCoeff
}
private fun fromStrength(mul: Int, add: Int)     = { bProps: BasicProperties -> bProps.strength * mul + add }
private fun fromDexterity(mul: Int, add: Int)    = { bProps: BasicProperties -> bProps.dexterity * mul + add }
private fun fromConstitution(mul: Int, add: Int) = { bProps: BasicProperties -> bProps.constitution * mul + add }
private fun fromIntelligence(mul: Int, add: Int) = { bProps: BasicProperties -> bProps.intelligence * mul + add }
private fun fromWisdom(mul: Int, add: Int)       = { bProps: BasicProperties -> bProps.wisdom * mul + add }
private fun fromCharisma(mul: Int, add: Int)     = { bProps: BasicProperties -> bProps.charisma * mul + add }

val characterPropertiesList: Map<String, (BasicProperties) -> Int> = mapOf(
    "melee_at" to fromStrength(6, 30), // FIXME: подобрать адекватные коэффициенты
    "dist_at" to fromDexterity(3, 20), // примечание: я не играл в D&D
    "max_hp" to fromConstitution(10, 100),
    "mag_at" to fromIntelligence(15, 120),
    "max_mp" to fromWisdom(20, 150),
    "initiative" to fromCharisma(3, 16),
    "speed" to constant(5),
    "mana_cost" to constant(25),
    "dist" to constant(16)
)
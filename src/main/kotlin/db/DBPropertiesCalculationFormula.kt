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

// FIXME: подобрать адекватные коэффициенты и значения
val characterPropertiesList: Map<String, (BasicProperties) -> Int> = mapOf(
    "Max health" to fromConstitution(10, 100),
    "Max mana" to fromWisdom(20, 150),

    // при создании на максимум
    "Current health" to fromConstitution(10, 100),
    "Current mana" to fromWisdom(20, 150),

    "Melee attack damage" to fromStrength(6, 30),
    "Ranged attack damage" to fromDexterity(3, 20),

    "Magic attack damage" to fromIntelligence(15, 120),
    "Magic attack cost" to constant(25),

    "Ranged and magic attack distance" to constant(16),

    "Initiative" to fromCharisma(3, 16),
    "Speed" to constant(5)
)

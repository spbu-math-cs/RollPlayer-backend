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
    "MAX_HP" to fromConstitution(4, 32),
    "MAX_MP" to fromWisdom(5, 20),

    // при создании на максимум
    "CURR_HP" to fromConstitution(4, 32),
    "CURR_MP" to fromWisdom(5, 20),

    "MELEE_AT_DMG" to fromStrength(2, 16),
    "RANGED_AT_DMG" to fromDexterity(2, 8),

    "MAGIC_AT_DMG" to fromIntelligence(2, 24),
    "MAGIC_AT_COST" to constant(20),

    "RANGED_AT_DIST" to constant(16),
    "MAGIC_AT_DIST" to constant(16),

    "INIT" to fromCharisma(1, 10),
    "SPEED" to constant(5)
)

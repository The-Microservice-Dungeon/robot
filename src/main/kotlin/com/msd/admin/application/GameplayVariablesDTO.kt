package com.msd.admin.application

class GameplayVariablesDTO(
    val storage: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    val damage: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    val hp: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    val miningSpeed: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    val energyCapacity: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    val energyRegeneration: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),

    val energyCostCalculation: Map<EnergyCostCalculationVerbs, Double> = mapOf(),
)

package com.msd.admin.application

class GameplayVariablesDTO(
    var storage: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    var damage: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    var hp: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    var miningSpeed: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    var energyCapacity: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),
    var energyRegeneration: Map<GameplayVariablesLevelVerbs, Int> = mapOf(),

    var energyCostCalculation: Map<EnergyCostCalculationVerbs, Double> = mapOf(),
)

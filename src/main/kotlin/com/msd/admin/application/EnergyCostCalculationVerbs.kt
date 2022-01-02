package com.msd.admin.application

enum class EnergyCostCalculationVerbs(val verb: String) {
    BLOCKING_BASE_COST("blockingBaseCost"),
    BLOCKING_MAX_ENERGY_PROPORTION("blockingMaxEnergyProportion"),
    MINING_MULTIPLIER("miningMultiplier"),
    MINING_WEIGHT("miningWeight"),
    MOVEMENT_MULTIPLIER("movementMultiplier"),
    ATTACKING_MULTIPLIER("attackingMultiplier"),
    ATTACKING_WEIGHT("attackingWeight")
}

package com.msd.admin.application

enum class EnergyCostCalculationVerbs(val verb: String) {
    BLOCKINGBASECOST("blockingBaseCost"),
    BLOCKINGMAXENERGYPROPORTION("blockingMaxEnergyProportion"),
    MININGMULTIPLIER("miningMultiplier"),
    MINGINGWEIGHT("miningWeight"),
    MOVEMENTMULTIPLIER("movementMultiplier"),
    ATTACKINGMULTIPLIER("attackingMultiplier"),
    ATTACKINGWEIGHT("attackingWeight")
}

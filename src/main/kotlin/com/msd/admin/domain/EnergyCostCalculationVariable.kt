package com.msd.admin.domain

import javax.persistence.Embeddable

@Embeddable
class EnergyCostCalculationVariable : GameplayVariable() {
    override val type = GameplayVariableType.ENERGYCOSTCALCULATION

    val blockingBaseCost: Int = 2
    val blockingMaxEnergyProportion: Int = 10
    val miningMultiplier: Int = 1
    val miningWeight: Int = 1
    val movementMultiplier: Int = 1
    val attackingMultiplier: Int = 1
    val attackingWeight: Int = 1
}

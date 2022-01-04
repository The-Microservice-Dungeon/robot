package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.EnergyCostCalculationVerbs
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
class EnergyCostCalculationValues {
    @Id
    val id: String = "ENERGY_COST_CALCULATION"

    @ElementCollection(fetch = FetchType.EAGER)
    var energyCostValues = mutableMapOf(
        EnergyCostCalculationVerbs.BLOCKING_BASE_COST to 2.0,
        EnergyCostCalculationVerbs.BLOCKING_MAX_ENERGY_PROPORTION to 0.1,
        EnergyCostCalculationVerbs.MINING_MULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.MINING_WEIGHT to 1.0,
        EnergyCostCalculationVerbs.MOVEMENT_MULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKING_MULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKING_WEIGHT to 1.0
    )
}

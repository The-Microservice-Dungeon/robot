package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.EnergyCostCalculationVerbs
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
class EnergyCostCalculationValue {
    @Id
    val id: String = "ENERGYCOSTCALCULATION"

    @ElementCollection(fetch = FetchType.EAGER)
    val levels = mutableMapOf(
        EnergyCostCalculationVerbs.BLOCKINGBASECOST to 2.0,
        EnergyCostCalculationVerbs.BLOCKINGMAXENERGYPROPORTION to 10.0,
        EnergyCostCalculationVerbs.MININGMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.MINGINGWEIGHT to 1.0,
        EnergyCostCalculationVerbs.MOVEMENTMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKINGMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKINGWEIGHT to 1.0
    )
}

object EnergyCostCalculationValueObject {
    var levels = mapOf(
        EnergyCostCalculationVerbs.BLOCKINGBASECOST to 2.0,
        EnergyCostCalculationVerbs.BLOCKINGMAXENERGYPROPORTION to 10.0,
        EnergyCostCalculationVerbs.MININGMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.MINGINGWEIGHT to 1.0,
        EnergyCostCalculationVerbs.MOVEMENTMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKINGMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKINGWEIGHT to 1.0
    )
}

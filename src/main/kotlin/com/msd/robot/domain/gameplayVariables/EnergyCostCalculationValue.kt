package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.EnergyCostCalculationVerbs
import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class EnergyCostCalculationValue {
    @Id
    val Id = UUID.randomUUID()

    @ElementCollection
    val values = mutableMapOf(
        EnergyCostCalculationVerbs.BLOCKINGBASECOST to 2.0,
        EnergyCostCalculationVerbs.BLOCKINGMAXENERGYPROPORTION to 10.0,
        EnergyCostCalculationVerbs.MININGMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.MINGINGWEIGHT to 1.0,
        EnergyCostCalculationVerbs.MOVEMENTMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKINGMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKINGWEIGHT to 1.0
    )
}

object EnergyCostCalculationObject {
    var values = mapOf(
        EnergyCostCalculationVerbs.BLOCKINGBASECOST to 2.0,
        EnergyCostCalculationVerbs.BLOCKINGMAXENERGYPROPORTION to 10.0,
        EnergyCostCalculationVerbs.MININGMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.MINGINGWEIGHT to 1.0,
        EnergyCostCalculationVerbs.MOVEMENTMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKINGMULTIPLIER to 1.0,
        EnergyCostCalculationVerbs.ATTACKINGWEIGHT to 1.0
    )
}

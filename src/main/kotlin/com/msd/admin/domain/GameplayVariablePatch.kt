package com.msd.admin.domain

import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class GameplayVariablePatch {
    @Id
    val id = UUID.randomUUID()

    @ElementCollection
    private val gamplayVariables = mutableMapOf(GameplayVariableType.DAMAGE to intArrayOf(1, 2, 3, 4, 5))

    // @OneToOne
    private val energyCostCalculationVariable: EnergyCostCalculationVariable = EnergyCostCalculationVariable()
}

package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.GameplayVariablesLevelVerbs
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
class EnergyRegenerationLevel {
    @Id
    val id: String = "ENERGYREGENERATION"

    @ElementCollection(fetch = FetchType.EAGER)
    val levels = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 4,
        GameplayVariablesLevelVerbs.LVL1 to 6,
        GameplayVariablesLevelVerbs.LVL2 to 8,
        GameplayVariablesLevelVerbs.LVL3 to 10,
        GameplayVariablesLevelVerbs.LVL4 to 15,
        GameplayVariablesLevelVerbs.LVL5 to 20
    )
}

object EnergyRegenerationLevelObject {
    var levels = mapOf(
        GameplayVariablesLevelVerbs.LVL0 to 4,
        GameplayVariablesLevelVerbs.LVL1 to 6,
        GameplayVariablesLevelVerbs.LVL2 to 8,
        GameplayVariablesLevelVerbs.LVL3 to 10,
        GameplayVariablesLevelVerbs.LVL4 to 15,
        GameplayVariablesLevelVerbs.LVL5 to 20
    )
}

package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.GameplayVariablesLevelVerbs
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
class EnergyCapacityLevel {
    @Id
    val id: String = "ENERGYCAPACITY"

    @ElementCollection(fetch = FetchType.EAGER)
    val levels = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 20,
        GameplayVariablesLevelVerbs.LVL1 to 30,
        GameplayVariablesLevelVerbs.LVL2 to 40,
        GameplayVariablesLevelVerbs.LVL3 to 60,
        GameplayVariablesLevelVerbs.LVL4 to 100,
        GameplayVariablesLevelVerbs.LVL5 to 200
    )
}

object EnergyCapacityLevelObject {
    var levels = mapOf(
        GameplayVariablesLevelVerbs.LVL0 to 20,
        GameplayVariablesLevelVerbs.LVL1 to 30,
        GameplayVariablesLevelVerbs.LVL2 to 40,
        GameplayVariablesLevelVerbs.LVL3 to 60,
        GameplayVariablesLevelVerbs.LVL4 to 100,
        GameplayVariablesLevelVerbs.LVL5 to 200
    )
}

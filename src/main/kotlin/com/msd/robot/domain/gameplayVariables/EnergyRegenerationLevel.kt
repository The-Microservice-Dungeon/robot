package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.GameplayVariablesLevelVerbs
import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class EnergyRegenerationLevel {
    @Id
    val Id = UUID.randomUUID()

    @ElementCollection
    val levels = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 4,
        GameplayVariablesLevelVerbs.LVL1 to 6,
        GameplayVariablesLevelVerbs.LVL2 to 8,
        GameplayVariablesLevelVerbs.LVL3 to 10,
        GameplayVariablesLevelVerbs.LVL4 to 15,
        GameplayVariablesLevelVerbs.LVL5 to 20
    )
}

object EnergyRegenerationObject {
    var levels = mapOf(
        GameplayVariablesLevelVerbs.LVL0 to 4,
        GameplayVariablesLevelVerbs.LVL1 to 6,
        GameplayVariablesLevelVerbs.LVL2 to 8,
        GameplayVariablesLevelVerbs.LVL3 to 10,
        GameplayVariablesLevelVerbs.LVL4 to 15,
        GameplayVariablesLevelVerbs.LVL5 to 20
    )
}

package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.GameplayVariablesLevelVerbs
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
class MiningSpeedLevel {
    @Id
    val id: String = "MININGSPEED"

    @ElementCollection(fetch = FetchType.EAGER)
    val levels = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 2,
        GameplayVariablesLevelVerbs.LVL1 to 5,
        GameplayVariablesLevelVerbs.LVL2 to 10,
        GameplayVariablesLevelVerbs.LVL3 to 15,
        GameplayVariablesLevelVerbs.LVL4 to 20,
        GameplayVariablesLevelVerbs.LVL5 to 40
    )
}

object MiningSpeedLevelObject {
    var levels = mapOf(
        GameplayVariablesLevelVerbs.LVL0 to 2,
        GameplayVariablesLevelVerbs.LVL1 to 5,
        GameplayVariablesLevelVerbs.LVL2 to 10,
        GameplayVariablesLevelVerbs.LVL3 to 15,
        GameplayVariablesLevelVerbs.LVL4 to 20,
        GameplayVariablesLevelVerbs.LVL5 to 40
    )
}

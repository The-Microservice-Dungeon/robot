package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.GameplayVariablesLevelVerbs
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
class DamageLevel {
    @Id
    val id: String = "DAMAGE"

    // TODO("STRING ??")

    @ElementCollection(fetch = FetchType.EAGER)
    val levels = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 1,
        GameplayVariablesLevelVerbs.LVL1 to 2,
        GameplayVariablesLevelVerbs.LVL2 to 5,
        GameplayVariablesLevelVerbs.LVL3 to 10,
        GameplayVariablesLevelVerbs.LVL4 to 20,
        GameplayVariablesLevelVerbs.LVL5 to 50
    )
}

object DamageLevelObject {
    var levels = mapOf(
        GameplayVariablesLevelVerbs.LVL0 to 1,
        GameplayVariablesLevelVerbs.LVL1 to 2,
        GameplayVariablesLevelVerbs.LVL2 to 5,
        GameplayVariablesLevelVerbs.LVL3 to 10,
        GameplayVariablesLevelVerbs.LVL4 to 20,
        GameplayVariablesLevelVerbs.LVL5 to 50
    )
}

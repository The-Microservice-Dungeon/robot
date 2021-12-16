package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.GameplayVariablesLevelVerbs
import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class HealthLevel {
    @Id
    val Id = UUID.randomUUID()

    @ElementCollection
    val levels = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 10,
        GameplayVariablesLevelVerbs.LVL1 to 25,
        GameplayVariablesLevelVerbs.LVL2 to 50,
        GameplayVariablesLevelVerbs.LVL3 to 100,
        GameplayVariablesLevelVerbs.LVL4 to 200,
        GameplayVariablesLevelVerbs.LVL5 to 500
    )
}

object HealthLevelObject {
    var levels = mapOf(
        GameplayVariablesLevelVerbs.LVL0 to 10,
        GameplayVariablesLevelVerbs.LVL1 to 25,
        GameplayVariablesLevelVerbs.LVL2 to 50,
        GameplayVariablesLevelVerbs.LVL3 to 100,
        GameplayVariablesLevelVerbs.LVL4 to 200,
        GameplayVariablesLevelVerbs.LVL5 to 500
    )
}

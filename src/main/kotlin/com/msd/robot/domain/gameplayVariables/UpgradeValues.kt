package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.GameplayVariablesLevelVerbs
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
class UpgradeValues {
    @Id
    val id: String = "VALUES"

    @ElementCollection(fetch = FetchType.EAGER)
    var damageValues = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 1,
        GameplayVariablesLevelVerbs.LVL1 to 2,
        GameplayVariablesLevelVerbs.LVL2 to 5,
        GameplayVariablesLevelVerbs.LVL3 to 10,
        GameplayVariablesLevelVerbs.LVL4 to 20,
        GameplayVariablesLevelVerbs.LVL5 to 50
    )

    @ElementCollection(fetch = FetchType.EAGER)
    var energyCapacityValues = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 20,
        GameplayVariablesLevelVerbs.LVL1 to 30,
        GameplayVariablesLevelVerbs.LVL2 to 40,
        GameplayVariablesLevelVerbs.LVL3 to 60,
        GameplayVariablesLevelVerbs.LVL4 to 100,
        GameplayVariablesLevelVerbs.LVL5 to 200
    )

    @ElementCollection(fetch = FetchType.EAGER)
    var energyRegenerationValues = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 4,
        GameplayVariablesLevelVerbs.LVL1 to 6,
        GameplayVariablesLevelVerbs.LVL2 to 8,
        GameplayVariablesLevelVerbs.LVL3 to 10,
        GameplayVariablesLevelVerbs.LVL4 to 15,
        GameplayVariablesLevelVerbs.LVL5 to 20
    )

    @ElementCollection(fetch = FetchType.EAGER)
    var healthValues = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 10,
        GameplayVariablesLevelVerbs.LVL1 to 25,
        GameplayVariablesLevelVerbs.LVL2 to 50,
        GameplayVariablesLevelVerbs.LVL3 to 100,
        GameplayVariablesLevelVerbs.LVL4 to 200,
        GameplayVariablesLevelVerbs.LVL5 to 500
    )

    @ElementCollection(fetch = FetchType.EAGER)
    var miningSpeedValues = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 2,
        GameplayVariablesLevelVerbs.LVL1 to 5,
        GameplayVariablesLevelVerbs.LVL2 to 10,
        GameplayVariablesLevelVerbs.LVL3 to 15,
        GameplayVariablesLevelVerbs.LVL4 to 20,
        GameplayVariablesLevelVerbs.LVL5 to 40
    )

    @ElementCollection(fetch = FetchType.EAGER)
    var storageValues = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 20,
        GameplayVariablesLevelVerbs.LVL1 to 50,
        GameplayVariablesLevelVerbs.LVL2 to 100,
        GameplayVariablesLevelVerbs.LVL3 to 200,
        GameplayVariablesLevelVerbs.LVL4 to 400,
        GameplayVariablesLevelVerbs.LVL5 to 1000
    )
}

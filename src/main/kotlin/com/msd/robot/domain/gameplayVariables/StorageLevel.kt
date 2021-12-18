package com.msd.robot.domain.gameplayVariables

import com.msd.admin.application.GameplayVariablesLevelVerbs
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
class StorageLevel {
    @Id
    val id: String = "STORAGE"

    @ElementCollection(fetch = FetchType.EAGER)
    val levels = mutableMapOf(
        GameplayVariablesLevelVerbs.LVL0 to 20,
        GameplayVariablesLevelVerbs.LVL1 to 50,
        GameplayVariablesLevelVerbs.LVL2 to 100,
        GameplayVariablesLevelVerbs.LVL3 to 200,
        GameplayVariablesLevelVerbs.LVL4 to 400,
        GameplayVariablesLevelVerbs.LVL5 to 1000
    )
}

object StorageLevelObject {
    var levels = mapOf(
        GameplayVariablesLevelVerbs.LVL0 to 20,
        GameplayVariablesLevelVerbs.LVL1 to 50,
        GameplayVariablesLevelVerbs.LVL2 to 100,
        GameplayVariablesLevelVerbs.LVL3 to 200,
        GameplayVariablesLevelVerbs.LVL4 to 400,
        GameplayVariablesLevelVerbs.LVL5 to 1000
    )
}

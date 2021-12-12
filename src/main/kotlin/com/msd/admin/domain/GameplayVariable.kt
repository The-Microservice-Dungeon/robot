package com.msd.admin.domain

import javax.persistence.Embeddable

@Embeddable
abstract class GameplayVariable {
    abstract val type: GameplayVariableType
}

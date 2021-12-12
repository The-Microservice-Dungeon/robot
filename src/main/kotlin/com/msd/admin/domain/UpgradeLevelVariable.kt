package com.msd.admin.domain

import javax.persistence.Embeddable

@Embeddable
class UpgradeLevelVariable(override val type: GameplayVariableType, val values: IntArray) : GameplayVariable() {
    private var lvl0: Int = 20
    private var lvl1: Int = 50
    private var lvl2: Int = 100
    private var lvl3: Int = 200
    private var lvl4: Int = 400
    private var lvl5: Int = 1000

    init {
        TODO("Check values")

        lvl0 = values[0]
        lvl1 = values[1]
        lvl2 = values[2]
        lvl3 = values[3]
        lvl4 = values[4]
        lvl5 = values[5]
    }

    fun getUpgradeValues(): IntArray {
        return intArrayOf(lvl0, lvl1, lvl2, lvl3, lvl4, lvl5)
    }
}

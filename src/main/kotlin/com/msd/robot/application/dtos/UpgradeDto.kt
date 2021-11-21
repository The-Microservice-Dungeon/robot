package com.msd.robot.application.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import com.msd.robot.domain.UpgradeType
import java.util.*

class UpgradeDto(
    val transaction_id: UUID,

    @JsonProperty("upgrade-type")
    val upgradeType: UpgradeType,

    @JsonProperty("target-level")
    val targetLevel: Int
)

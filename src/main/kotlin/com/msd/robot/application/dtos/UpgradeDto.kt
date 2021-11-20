package com.msd.robot.application.dtos

import com.msd.robot.domain.UpgradeType
import java.util.*

class UpgradeDto(
    val transaction_id: UUID,
    val `upgrade-type`: UpgradeType,
    val `target-level`: Int
)

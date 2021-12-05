package com.msd.event.application.dto

import java.util.*

data class RepairEventRobotDTO(
    val robotId: UUID,
    val remainingHealth: Int
)

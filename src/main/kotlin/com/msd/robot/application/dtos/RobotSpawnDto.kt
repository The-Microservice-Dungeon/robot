package com.msd.robot.application.dtos

import java.util.*

class RobotSpawnDto(
    val transaction_id: UUID,
    val player: UUID,
    val planet: UUID
)

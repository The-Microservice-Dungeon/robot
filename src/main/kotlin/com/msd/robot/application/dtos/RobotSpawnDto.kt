package com.msd.robot.application.dtos

import java.util.*

class RobotSpawnDto(
    val transactionId: UUID,
    val player: UUID,
    val planets: List<UUID>,
    val quantity: Int
)

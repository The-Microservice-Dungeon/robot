package com.msd.robot.application

import com.msd.domain.ResourceType
import com.msd.robot.domain.Robot
import java.util.*

class MiningProcessData(
    val robot: Robot,
    val planet: UUID,
    val transactionId: UUID,
    var resource: ResourceType,
    var amountRequested: Int,
)

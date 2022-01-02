package com.msd.event.application.dto

import java.util.*

class RobotDestroyedEventDTO(
    val robotId: UUID,
    val playerId: UUID
) : GenericEventDTO()

package com.msd.event.application.dto

import java.util.*

class SpawnEventDTO(
    val robotId: UUID,
    val playerId: UUID,
    val otherSeeableRobots: List<UUID>
) : GenericEventDTO()

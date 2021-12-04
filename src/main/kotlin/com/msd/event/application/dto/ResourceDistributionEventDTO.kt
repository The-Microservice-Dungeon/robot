package com.msd.event.application.dto

import java.util.*

class ResourceDistributionEventDTO(
    val robotId: UUID,
    val coal: Int,
    val iron: Int,
    val gem: Int,
    val gold: Int,
    val platin: Int
) : GenericEventDTO()

package com.msd.event.application.dto

import com.msd.domain.ResourceType

class MiningEventDTO(
    success: Boolean,
    message: String,
    val remainingEnergy: Int?,
    val updatedInventory: Int,
    val resourceType: ResourceType?
) : EventDTO(success, message)

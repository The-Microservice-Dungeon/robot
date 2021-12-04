package com.msd.application.dto

class MiningEventDTO(
    success: Boolean,
    message: String,
    val remainingEnergy: Int,
    val updatedInventory: Int,
    val resourceType: String
) : EventDTO(success, message)

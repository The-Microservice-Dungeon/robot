package com.msd.event.application.dto

class EnergyRegenEventDTO(
    success: Boolean,
    message: String,
    val remainingEnergy: Int?
) : EventDTO(success, message)

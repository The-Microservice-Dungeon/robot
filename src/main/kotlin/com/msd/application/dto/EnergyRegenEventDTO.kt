package com.msd.application.dto

class EnergyRegenEventDTO(
    success: Boolean,
    message: String,
    val remainingEnergy: Int?
) : EventDTO(success, message)

package com.msd.event.application.dto

class RegenerationEventDTO(
    success: Boolean,
    message: String,
    val remainingEnergy: Int?
) : EventDTO(success, message)

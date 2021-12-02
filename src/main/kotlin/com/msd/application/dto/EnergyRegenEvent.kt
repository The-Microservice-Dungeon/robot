package com.msd.application.dto

class EnergyRegenEvent(
    success: Boolean,
    message: String,
    val remainingEnergy: Int?
) : EventDTO(success, message) {

}

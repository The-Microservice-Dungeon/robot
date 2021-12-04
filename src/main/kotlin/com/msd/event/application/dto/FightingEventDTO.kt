package com.msd.event.application.dto

import java.util.*

class FightingEventDTO(
    success: Boolean,
    message: String,
    val attacker: UUID?,
    val defender: UUID?,
    val remainingDefenderHealth: Int?,
    val remainingEnergy: Int?
) : EventDTO(success, message)

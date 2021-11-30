package com.msd.application.dto

import com.msd.application.EventType
import com.msd.planet.application.PlanetDTO
import java.util.*

class MovementEventDTO(
    success: Boolean,
    message: String,
    eventType: EventType,
    val energyChangedBy: Int,
    val planet: PlanetDTO?,
    val robots: List<UUID> = listOf()
) : EventDTO(success, message, eventType)

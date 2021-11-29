package com.msd.application.dto

import com.msd.planet.application.PlanetDTO
import java.util.*

class MovementEventDTO(
    success: Boolean,
    message: String,
    val energyChangedBy: Int,
    val planet: PlanetDTO?,
    val robots: List<UUID> = listOf()
) : EventDTO(success, message)

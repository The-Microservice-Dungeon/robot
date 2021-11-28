package com.msd.application

import com.msd.planet.domain.Planet
import java.util.*

class GameMapPlanetDto(
    val id: UUID,
    val movement_difficulty: Int,
    val resource: ResourceDto? = null
) {

    fun toPlanet(): Planet {
        return Planet(id)
    }
}

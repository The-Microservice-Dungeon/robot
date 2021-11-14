package com.msd.application

import com.msd.planet.domain.Planet
import java.util.*

class GameMapPlanetDto(
    val id: UUID,
    val movementCost: Int
) {

    fun toPlanet(): Planet {
        return Planet(id)
    }
}

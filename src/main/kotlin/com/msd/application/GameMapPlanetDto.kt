package com.msd.application

import com.fasterxml.jackson.annotation.JsonProperty
import com.msd.planet.domain.Planet
import java.util.*

class GameMapPlanetDto(
    val id: UUID,
    @JsonProperty("movement_difficulty")
    val movementDifficulty: Int,
    val resource: ResourceDto? = null
) {

    fun toPlanet(): Planet {
        return Planet(id)
    }
}

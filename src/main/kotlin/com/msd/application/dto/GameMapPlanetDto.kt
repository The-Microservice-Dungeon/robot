package com.msd.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetType
import java.util.*

class GameMapPlanetDto(
    val id: UUID,
    @JsonProperty("movement_difficulty")
    val movementDifficulty: Int,
    val planetType: PlanetType,
    val resource: ResourceDto? = null,
    val neighbours: List<GameMapNeighbourDto> = listOf()
) {

    fun toPlanet(): Planet {
        return Planet(id)
    }
}

package com.msd.application.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.msd.planet.domain.Planet
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class GameMapPlanetDto(
    val id: UUID,
    @JsonProperty("movement_difficulty")
    val movementDifficulty: Int,
    val spacestation: Boolean = false,
    val resource: ResourceDto? = null,
    val neighbours: List<GameMapNeighbourDto> = listOf()
) {

    fun toPlanet(): Planet {
        return Planet(id, resource?.resourceType)
    }
}

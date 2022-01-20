package com.msd.application.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.msd.planet.domain.MapDirection
import java.util.*

@JsonIgnoreProperties
class GameMapNeighbourDto(
    @JsonProperty("planet_id")
    val planetId: UUID,
    @JsonProperty("movement_difficulty")
    val movementDifficulty: Int,
    val direction: MapDirection
)

package com.msd.planet.application

import com.msd.domain.ResourceType
import com.msd.planet.domain.PlanetType
import java.util.*

data class PlanetDTO(
    val planetId: UUID,
    val movementDifficulty: Int,
    val planetType: PlanetType?,
    val resourceType: ResourceType?
)

package com.msd.planet.application

import com.msd.domain.ResourceType
import java.util.*

data class PlanetDTO(
    val planetId: UUID,
    val movementDifficulty: Int,
    val resourceType: ResourceType?
)

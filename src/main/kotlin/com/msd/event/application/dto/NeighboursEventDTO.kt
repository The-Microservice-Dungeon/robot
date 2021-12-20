package com.msd.event.application.dto

import com.msd.planet.domain.MapDirection
import java.util.*

class NeighboursEventDTO(
    val neighbours: List<NeighbourDTO>
) : GenericEventDTO() {

    class NeighbourDTO(
        val planetId: UUID,
        val movementDifficulty: Int,
        val direction: MapDirection
    )
}

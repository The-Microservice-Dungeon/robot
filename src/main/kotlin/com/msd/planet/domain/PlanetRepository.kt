package com.msd.planet.domain

import org.springframework.data.repository.CrudRepository
import java.util.*

interface PlanetRepository : CrudRepository<Planet, UUID> {

    fun findAllByBlocked(blocked: Boolean): List<Planet>
}

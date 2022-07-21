package com.msd.planet.domain

import org.springframework.stereotype.Service

@Service
class PlanetDomainService(
    val planetRepository: PlanetRepository
) {
  /*  fun resetBlocks() {
        val planets = planetRepository.findAllByBlocked(true)
        planets.forEach { it.blocked = false }
        planetRepository.saveAll(planets)
    }
    */
}

package com.msd.application

import com.msd.planet.domain.PlanetRepository
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class GameRoundService(
    val planetRepository: PlanetRepository
) {

    @KafkaListener(id = "gameRoundListener", topics = ["gameServiceRound"])
    fun resetBlocks() {
        val planets = planetRepository.findAllByBlocked(true)
        planets.forEach { it.blocked = false }
        planetRepository.saveAll(planets)
    }
}

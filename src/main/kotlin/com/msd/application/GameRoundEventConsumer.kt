package com.msd.application

import com.msd.planet.domain.PlanetRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class GameRoundEventConsumer(
    val planetRepository: PlanetRepository,
) {

    @KafkaListener(id = "gameRoundListener", topics = ["\${spring.kafka.topic.consumer.round}"])
    fun gameRoundListener(record: ConsumerRecord<String, String>) {
        val payload = record.value() // TODO update if the payload isn't just a string
        if (payload == "ended") {
            resetBlocks()
        }
    }

    fun resetBlocks() {
        val planets = planetRepository.findAllByBlocked(true)
        planets.forEach { it.blocked = false }
        planetRepository.saveAll(planets)
    }
}

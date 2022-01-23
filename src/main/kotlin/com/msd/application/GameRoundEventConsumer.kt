package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.dto.RoundStatusDTO
import com.msd.planet.domain.PlanetDomainService
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class GameRoundEventConsumer(
    val planetDomainService: PlanetDomainService,
) {
    private val logger = KotlinLogging.logger {}

    @KafkaListener(id = "gameRoundListener", topics = ["\${spring.kafka.topic.consumer.round}"])
    fun gameRoundListener(record: ConsumerRecord<String, String>) {
        val payload = jacksonObjectMapper().readValue(record.value(), RoundStatusDTO::class.java)
        logger.info("Handling game round event(Round " + payload.roundNumber + "): " + payload.roundStatus)
        if (payload.roundStatus == RoundStatus.ENDED) {
            planetDomainService.resetBlocks()
            logger.info("Reset blocked planets")
        }
    }
}

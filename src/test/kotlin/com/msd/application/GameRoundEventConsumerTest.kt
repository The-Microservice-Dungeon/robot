package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.dto.RoundStatusDTO
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetRepository
import com.msd.robot.domain.gameplayVariables.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import java.util.*
import javax.transaction.Transactional

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"])
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["test"])
internal class GameRoundEventConsumerTest(
    @Autowired val kafkaTemplate: KafkaTemplate<String, String>,
    @Autowired val gameRoundEventConsumer: GameRoundEventConsumer,
    @Autowired val planetRepository: PlanetRepository,
    @Autowired private var mockMvc: MockMvc
) {

    private lateinit var planet1: Planet
    private lateinit var planet2: Planet
    private lateinit var planet3: Planet
    private lateinit var planet4: Planet
    private lateinit var planet5: Planet
    private lateinit var planet6: Planet
    private lateinit var planets: List<Planet>

    @Value("\${spring.kafka.topic.consumer.round}")
    private lateinit var roundTopic: String

    @BeforeEach
    fun setup() {
        planet1 = Planet(UUID.randomUUID())
        planet2 = Planet(UUID.randomUUID())
        planet3 = Planet(UUID.randomUUID())
        planet4 = Planet(UUID.randomUUID())
        planet5 = Planet(UUID.randomUUID())
        planet6 = Planet(UUID.randomUUID())
        planets = listOf(planet1, planet2, planet3, planet4, planet5, planet6)
    }

    @Test
    fun `Planets get unblocked when gameRoundListener Method is called`() {
        // given
        planets.forEach { it.blocked = true }
        planetRepository.saveAll(planets)
        val record = ConsumerRecord(roundTopic, 1, 0, "", jacksonObjectMapper().writeValueAsString(RoundStatusDTO(0, RoundStatus.ENDED, UUID.randomUUID())))
        // when
        gameRoundEventConsumer.gameRoundListener(record)
        // then
        assertAll(
            planetRepository.findAll().map { { assertFalse(it.blocked) } }
        )
    }

    @Test
    fun `planets don't get unblocked when value is not ended`() {
        // given
        planets.forEach { it.blocked = true }
        planetRepository.saveAll(planets)
        val record = ConsumerRecord(roundTopic, 1, 0, "", jacksonObjectMapper().writeValueAsString(RoundStatusDTO(0, RoundStatus.STARTED, UUID.randomUUID())))
        // when
        gameRoundEventConsumer.gameRoundListener(record)
        // then
        assertAll(
            planetRepository.findAll().map { { assertTrue(it.blocked) } }
        )
    }

//    @Test
//    fun `Planets get unblocked after round ended`() {
//        // given
//        planets.forEach { it.blocked = true }
//        planetRepository.saveAll(planets)
//
//        // when
//        kafkaTemplate.send(ProducerRecord("gameServiceRound", "ended"))
//        // then
//        assertAll(
//            planetRepository.findAll().map { { assertFalse(it.blocked) } }
//        )
//    }
}

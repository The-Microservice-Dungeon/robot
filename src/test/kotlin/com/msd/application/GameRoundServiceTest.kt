package com.msd.application

import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetRepository
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.util.*

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"])
internal class GameRoundServiceTest(
    @Autowired val kafkaTemplate: KafkaTemplate<String, String>,
    @Autowired val registry: KafkaListenerEndpointRegistry,
    @Autowired val gameRoundService: GameRoundService,
    @Autowired val planetRepository: PlanetRepository
) {

    private lateinit var planet1: Planet
    private lateinit var planet2: Planet
    private lateinit var planet3: Planet
    private lateinit var planet4: Planet
    private lateinit var planet5: Planet
    private lateinit var planet6: Planet
    private lateinit var planets: List<Planet>

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
    fun `Planets get unblocked when resetBlocks Method is called`() {
        // given
        planets.forEach { it.blocked = true }
        planetRepository.saveAll(planets)
        // when
        gameRoundService.resetBlocks()
        // then
        assertAll(
            planetRepository.findAll().map { { assertFalse(it.blocked) } }
        )
    }

    @Test
    fun `Planets get unblocked after round ended`() {
        // given
        planets.forEach { it.blocked = true }
        planetRepository.saveAll(planets)

        // when
        kafkaTemplate.send(ProducerRecord("gameServiceRound", "ended"))
        // then
        assertAll(
            planetRepository.findAll().map { { assertFalse(it.blocked) } }
        )
    }
}

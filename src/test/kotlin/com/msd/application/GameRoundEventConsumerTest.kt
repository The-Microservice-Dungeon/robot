package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.admin.application.EnergyCostCalculationVerbs
import com.msd.admin.application.GameplayVariablesDTO
import com.msd.admin.application.GameplayVariablesLevelVerbs
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetRepository
import com.msd.robot.domain.Robot
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
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
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
        val record = ConsumerRecord(roundTopic, 1, 0, "", "ended")
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
        val record = ConsumerRecord(roundTopic, 1, 0, "", "started")
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

    @Test
    fun `UpgradeValues get patched when GameRound ends (gameRoundListener-Method is called)`() {
        // given

        val patchDTO = GameplayVariablesDTO()
        patchDTO.damage = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.energyCapacity = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.energyCostCalculation = mutableMapOf(
            EnergyCostCalculationVerbs.ATTACKINGWEIGHT to 1.0,
            EnergyCostCalculationVerbs.ATTACKINGMULTIPLIER to 1.0,
            EnergyCostCalculationVerbs.MOVEMENTMULTIPLIER to 1.0,
            EnergyCostCalculationVerbs.MININGMULTIPLIER to 1.0,
            EnergyCostCalculationVerbs.MINGINGWEIGHT to 1.0,
            EnergyCostCalculationVerbs.BLOCKINGMAXENERGYPROPORTION to 1.0,
            EnergyCostCalculationVerbs.BLOCKINGBASECOST to 2.0
        )

        patchDTO.energyRegeneration = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.hp = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.miningSpeed = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.storage = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        mockMvc.patch("/gameplay-variables") {
            contentType = MediaType.APPLICATION_JSON
            content = jacksonObjectMapper().writeValueAsString(patchDTO)
        }.andExpect {
            status { isOk() }
        }

        val oldRobot = Robot(UUID.randomUUID(), planet1)

        assertAll(
            { assert(oldRobot.inventory.maxStorage != 0) },
            { assert(oldRobot.health != 0) },
            { assert(oldRobot.attackDamage != 0) },
            { assert(oldRobot.miningSpeed != 0) },
            { assert(oldRobot.maxEnergy != 0) },
            { assert(oldRobot.energyRegen != 0) }
        )

        // when
        val record = ConsumerRecord(roundTopic, 1, 0, "", "ended")
        gameRoundEventConsumer.gameRoundListener(record)

        // then
        val newRobot = Robot(UUID.randomUUID(), planet1)

        assertAll(
            { assert(newRobot.inventory.maxStorage == 0) },
            { assert(newRobot.health == 0) },
            { assert(newRobot.attackDamage == 0) },
            { assert(newRobot.miningSpeed == 0) },
            { assert(newRobot.maxEnergy == 0) },
            { assert(newRobot.energyRegen == 0) },

            { assert(EnergyCostCalculationValueObject.levels[EnergyCostCalculationVerbs.BLOCKINGBASECOST] == 2.0) }
        )
    }
}

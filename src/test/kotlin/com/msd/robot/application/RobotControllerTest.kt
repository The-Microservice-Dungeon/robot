package com.msd.robot.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.msd.application.AbstractKafkaProducerTest
import com.msd.application.dto.GameMapPlanetDto
import com.msd.application.dto.ResourceDto
import com.msd.domain.DomainEvent
import com.msd.domain.ResourceType
import com.msd.event.application.EventType
import com.msd.event.application.ProducerTopicConfiguration
import com.msd.event.application.dto.SpawnEventDTO
import com.msd.planet.domain.Planet
import com.msd.robot.application.dtos.RestorationDTO
import com.msd.robot.application.dtos.RobotDto
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.UpgradeType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["no-async", " test"])
@Transactional
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://\${spring.kafka.bootstrap-servers}", "port=9092"]
)
class RobotControllerTest(
    @Autowired private var mockMvc: MockMvc,
    @Autowired private var robotRepository: RobotRepository,
    @Autowired private var mapper: ObjectMapper,
    @Autowired private val embeddedKafka: EmbeddedKafkaBroker,
    @Autowired private val topicConfig: ProducerTopicConfiguration
) : AbstractKafkaProducerTest(embeddedKafka, topicConfig) {

    val player1Id: UUID = UUID.fromString("d43608d5-2107-47a0-bd4f-6720dfa53c4d")

    val planet1Id: UUID = UUID.fromString("8f3c39b1-c439-4646-b646-ace4839d8849")

    companion object {
        val mockGameServiceWebClient = MockWebServer()

        @BeforeAll
        @JvmStatic
        internal fun setUp() {
            mockGameServiceWebClient.start(port = 8081)
        }

        @AfterAll
        @JvmStatic
        internal fun tearDown() {
            mockGameServiceWebClient.shutdown()
        }
    }

    @Test
    fun `Sending Spawn Command with invalid planet UUID returns 400`() {
        val spawnDtoInvalidPlanetUUID = """
            {
                "transaction_id": "${UUID.randomUUID()}",
                "player": "$player1Id",
                "planet": "Invalid UUID"
            }
        """.trimIndent()

        mockMvc.post("/robots") {
            contentType = MediaType.APPLICATION_JSON
            content = spawnDtoInvalidPlanetUUID
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `Sending Spawn Command with invalid player UUID returns 400`() {
        val spawnDtoInvalidPlayerUUID = """
            {
                "transaction_id": "${UUID.randomUUID()}",
                "player": "Invalid UUID",
                "planet": "$planet1Id"
            }
        """.trimIndent()

        mockMvc.post("/robots") {
            contentType = MediaType.APPLICATION_JSON
            content = spawnDtoInvalidPlayerUUID
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `Sending Spawn Command with invalid player transaction UUID returns 400`() {
        val spawnDtoInvalidTransactionUUID = """
            {
                "transaction_id": "Invalid UUID",
                "player": "$player1Id",
                "planet": "$planet1Id"
            }
        """.trimIndent()

        mockMvc.post("/robots") {
            contentType = MediaType.APPLICATION_JSON
            content = spawnDtoInvalidTransactionUUID
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `Sending a spawn command leads to a robot being present in the repository`() {
        startSpawnContainer()

        val planet1GameMapDto = GameMapPlanetDto(planet1Id, 3, resource = ResourceDto(ResourceType.COAL))
        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(planet1GameMapDto))
                .setHeader("Content-Type", "application/json")
        )

        val transactionId = UUID.randomUUID()
        val spawnDto = """
            {
                "transactionId": "$transactionId",
                "player": "$player1Id",
                "planet": "$planet1Id",
                "quantity": 1
            }
        """.trimIndent()

        val results: List<RobotDto> = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = spawnDto
            }.andExpect {
                status { isCreated() }
            }.andReturn().response.contentAsString
        )

        val resultRobot = results[0]

        assert(robotRepository.existsById(resultRobot.id))

        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        Assertions.assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_SPAWNED, singleRecord.topic())
        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), SpawnEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(transactionId, EventType.ROBOT_SPAWNED, domainEvent)
        eventTestUtils.checkSpawnPayload(
            player1Id,
            listOf<UUID>(),
            domainEvent.payload
        )
    }

    @Test
    fun `passing a wrong UUID returns a 404 when restoring`() {
        // given
        val restorationDTO = """
            {
                "transactionId": "${UUID.randomUUID()}",
                "restorationType": "HEALTH"
            }
        """.trimIndent()
        val robotId = UUID.randomUUID()
        // when
        val result = mockMvc.post("/robots/$robotId/instant-restore") {
            contentType = MediaType.APPLICATION_JSON
            content = restorationDTO
        }.andExpect {
            status { HttpStatus.NOT_FOUND }
        }.andDo {
            print()
        }.andReturn()

        // then
        assertEquals("Robot with ID $robotId not found", result.response.contentAsString)
    }

    @Test
    fun `Passing a wrong RestorationType when restoring a Robot returns a 400`() {
        // given
        val restorationDTO = """
            {
                "transactionId": "${UUID.randomUUID()}",
                "restorationType": "WRONG"
            }
        """.trimIndent()
        val robot1 = Robot(player1Id, Planet(planet1Id))
        robotRepository.save(robot1)
        // when
        val result = mockMvc.post("/robots/${robot1.id}/instant-restore") {
            contentType = MediaType.APPLICATION_JSON
            content = restorationDTO
        }.andExpect {
            status { HttpStatus.BAD_REQUEST }
        }.andDo {
            print()
        }.andReturn()

        // then
        assertEquals("Request could not be accepted", result.response.contentAsString)
    }

    @Test
    fun `passing HEALTH RestorationType only restores health to full`() {
        // given
        val restorationDTO = RestorationDTO(UUID.randomUUID(), RestorationType.HEALTH)
        var robot1 = Robot(player1Id, Planet(planet1Id))
        robot1.move(Planet(UUID.randomUUID()), 10)
        robot1.receiveDamage(5)
        robotRepository.save(robot1)

        // when
        mockMvc.post("/robots/${robot1.id}/instant-restore") {
            contentType = MediaType.APPLICATION_JSON
            content = jacksonObjectMapper().writeValueAsString(restorationDTO)
        }.andExpect {
            status { HttpStatus.OK }
        }.andDo {
            print()
        }

        // then
        robot1 = robotRepository.findByIdOrNull(robot1.id)!!
        assertEquals(10, robot1.health)
        assertEquals(10, robot1.energy)
    }

    @Test
    fun `Passing ENERGY RestorationType only restores ENERGY to full`() {
        // given
        val restorationDTO = RestorationDTO(UUID.randomUUID(), RestorationType.ENERGY)
        var robot1 = Robot(player1Id, Planet(planet1Id))
        robot1.move(Planet(UUID.randomUUID()), 10)
        robot1.receiveDamage(5)
        robotRepository.save(robot1)
        // when
        mockMvc.post("/robots/${robot1.id}/instant-restore") {
            contentType = MediaType.APPLICATION_JSON
            content = jacksonObjectMapper().writeValueAsString(restorationDTO)
        }.andExpect {
            status { HttpStatus.OK }
        }.andDo {
            print()
        }

        // then
        robot1 = robotRepository.findByIdOrNull(robot1.id)!!
        assertEquals(20, robot1.energy)
        assertEquals(5, robot1.health)
    }

    @Test
    fun `Sending Upgrade Command with invalid player transaction UUID returns 400 and does not increase the upgrade level`() {
        // given
        val robot1 = robotRepository.save(Robot(player1Id, Planet(UUID.randomUUID())))

        val upgradeDto = """
            {
                "transaction_id": "Invalid UUID",
                "upgrade-type": "DAMAGE",
                "target-level": 1
            }
        """.trimIndent()

        assertEquals(robot1.damageLevel, 0)

        // when
        val result = mockMvc.post("/robots/${robot1.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = upgradeDto
        }.andExpect {
            status { isBadRequest() }
        }.andDo {
            print()
        }.andReturn()

        // then
        assertEquals("Request could not be accepted", result.response.contentAsString)
        assertEquals(robotRepository.findByIdOrNull(robot1.id)!!.damageLevel, 0)
    }

    @Test
    fun `Sending Upgrade Command with invalid UpgradeType returns 400`() {
        // given
        val robot1 = robotRepository.save(Robot(player1Id, Planet(UUID.randomUUID())))

        val upgradeDto = """
            {
                "transaction_id": "${UUID.randomUUID()}",
                "upgrade-type": "Invalid UpgradeType",
                "target-level": 1
            }
        """.trimIndent()

        // when
        val result = mockMvc.post("/robots/${robot1.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = upgradeDto
        }.andDo {
            print()
        }.andReturn()

        // then
        assertEquals("Request could not be accepted", result.response.contentAsString)
    }

    @Test
    fun `Sending Upgrade Command returns 404 when specified robot UUID is not found`() {
        // given
        val upgradeDto = """
            {
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "DAMAGE",
                "targetLevel": 1
            }
        """.trimIndent()

        // when
        mockMvc.post("/robots/${UUID.randomUUID()}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = upgradeDto
        }.andDo {
            print()
        }.andExpect { // then
            status { isNotFound() }
        }
    }

    @Test
    fun `Sending Upgrade Command that would skip upgrade levels returns 409 and does not increase the upgrade level`() {
        // given
        val robot1 = robotRepository.save(Robot(player1Id, Planet(UUID.randomUUID())))

        val upgradeDto = """
            {
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "DAMAGE",
                "targetLevel": 2
            }
        """.trimIndent()

        assertEquals(robot1.damageLevel, 0)

        // when
        mockMvc.post("/robots/${robot1.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = upgradeDto
        }.andDo {
            print()
        }.andExpect {
            status { isConflict() }
        }

        // then
        assertEquals(robotRepository.findByIdOrNull(robot1.id)!!.damageLevel, 0)
    }

    @Test
    fun `Sending Upgrade Command that would set upgrade levels over 5 returns 409 and does not increase the upgrade level`() {
        // given
        val robot1 = robotRepository.save(Robot(player1Id, Planet(UUID.randomUUID())))

        for (i in 1..5) {
            robot1.upgrade(UpgradeType.DAMAGE, i)
        }

        robotRepository.save(robot1)

        assertEquals(robot1.damageLevel, 5)

        val upgradeDto = """
            {
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "DAMAGE",
                "targetLevel": 6
            }
        """.trimIndent()

        // when
        mockMvc.post("/robots/${robot1.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = upgradeDto
        }.andDo {
            print()
        }.andExpect { // then
            status { isConflict() }
        }

        assertEquals(robotRepository.findByIdOrNull(robot1.id)!!.damageLevel, 5)
    }

    @Test
    fun `Sending Upgrade Command that would lower upgrade levels returns 409 and does not increase the upgrade level `() {
        // given
        val robot1 = robotRepository.save(Robot(player1Id, Planet(UUID.randomUUID())))
        robot1.upgrade(UpgradeType.DAMAGE, 1)
        robotRepository.save(robot1)

        assertEquals(robot1.damageLevel, 1)

        val upgradeDto = """
            {
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "DAMAGE",
                "targetLevel": 0
            }
        """.trimIndent()

        // when
        mockMvc.post("/robots/${robot1.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = upgradeDto
        }.andDo {
            print()
        }.andExpect { // then
            status { isConflict() }
        }

        assertEquals(robotRepository.findByIdOrNull(robot1.id)!!.damageLevel, 1)
    }

    @Test
    fun `Sending Upgrade Command correctly increases the given upgrade level`() {
        // given
        val robot1 = robotRepository.save(Robot(player1Id, Planet(UUID.randomUUID())))

        // when
        UpgradeType.values().forEach {
            val upgradeDto = """
            {
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "$it",
                "targetLevel": 1
            }
            """.trimIndent()

            mockMvc.post("/robots/${robot1.id}/upgrades") {
                contentType = MediaType.APPLICATION_JSON
                content = upgradeDto
            }.andDo {
                print()
            }.andExpect {
                status { HttpStatus.OK }
            }
        }

        val robot = robotRepository.findByIdOrNull(robot1.id)!!

        // then
        assertAll(
            { assertEquals(1, robot.damageLevel) },
            { assertEquals(1, robot.healthLevel) },
            { assertEquals(1, robot.energyLevel) },
            { assertEquals(1, robot.inventory.storageLevel) },
            { assertEquals(1, robot.miningLevel) },
            { assertEquals(1, robot.miningSpeedLevel) },
            { assertEquals(1, robot.energyRegenLevel) }
        )
    }
}

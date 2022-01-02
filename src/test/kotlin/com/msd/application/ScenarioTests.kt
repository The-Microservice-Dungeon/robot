package com.msd.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.msd.application.dto.GameMapNeighbourDto
import com.msd.application.dto.GameMapPlanetDto
import com.msd.application.dto.MineResponseDto
import com.msd.application.dto.ResourceDto
import com.msd.command.application.CommandDTO
import com.msd.domain.ResourceType
import com.msd.event.application.ProducerTopicConfiguration
import com.msd.item.domain.AttackItemType
import com.msd.planet.domain.MapDirection
import com.msd.planet.domain.PlanetRepository
import com.msd.robot.application.dtos.RobotDto
import com.msd.robot.application.dtos.RobotSpawnDto
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.gameplayVariables.UpgradeValues
import com.msd.robot.domain.getByVal
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["no-async", "test"])
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://\${spring.kafka.bootstrap-servers}", "port=9092"]
)
@Transactional
class ScenarioTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val mapper: ObjectMapper,
    @Autowired private val embeddedKafka: EmbeddedKafkaBroker,
    @Autowired private val topicConfig: ProducerTopicConfiguration,
    @Autowired private val robotRepo: RobotRepository,
    @Autowired private val planetRepo: PlanetRepository,
) : AbstractKafkaProducerTest(embeddedKafka, topicConfig) {

    val planet1 = UUID.randomUUID()
    val planet2 = UUID.randomUUID()
    val planet3 = UUID.randomUUID()

    val player1 = UUID.randomUUID()
    val player2 = UUID.randomUUID()

    val upgradeValues = UpgradeValues()

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

    /**
     * 1. Scenario:
     *
     * User creates 5 robots of two different players, moves them onto the same planet and lets
     * them fight each other until three robots get destroyed.
     */
    @Test
    fun `Movement and fighting Scenario`() {
        startMovementContainer()
        startNeighboursContainer()
        startFightingContainer()
        startResourceDistributionContainer()
        startRobotDestroyedContainer()
        consumerRecords.clear()

        // //////////////////////  1. Spawn the robots  //////////////////////////////
        // player1, all robots on planet1
        var robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player1, planet1)
        val robot1 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player1, planet1)
        val robot2 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player1, planet1)
        val robot3 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        // player2, all robots on on planet2
        robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player2, planet2)
        val robot4 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player2, planet2)
        val robot5 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        // ////////////////////////////////// 2. Move the robots to the same planet /////////////////////////
        // All robots move to planet3
        val planet3Dto = GameMapPlanetDto(
            planet3, 3,
            neighbours = listOf(
                GameMapNeighbourDto(planet1, 3, MapDirection.NORTH),
                GameMapNeighbourDto(planet2, 3, MapDirection.SOUTH)
            )
        )
        for (i in 1..5)
            mockGameServiceWebClient.enqueue(
                MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(planet3Dto))
            )

        val moveCommands = listOf(
            "move ${robot1.id} $planet3 ${UUID.randomUUID()}",
            "move ${robot2.id} $planet3 ${UUID.randomUUID()}",
            "move ${robot3.id} $planet3 ${UUID.randomUUID()}",
            "move ${robot4.id} $planet3 ${UUID.randomUUID()}",
            "move ${robot5.id} $planet3 ${UUID.randomUUID()}"
        )
        var commandDto = CommandDTO(moveCommands)
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(commandDto)
        }

        // /////////////////////////////////////// 3. Fight //////////////////////////////////////////////
        var fightCommands = listOf(
            "fight ${robot1.id} ${robot4.id} ${UUID.randomUUID()}",
            "fight ${robot2.id} ${robot4.id} ${UUID.randomUUID()}",
            "fight ${robot4.id} ${robot1.id} ${UUID.randomUUID()}",
            "fight ${robot5.id} ${robot2.id} ${UUID.randomUUID()}"
        )
        commandDto = CommandDTO(fightCommands)
        for (i in 1..5)
            mockMvc.post("/commands") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(commandDto)
            }
        // robot 4 ist dead now
        fightCommands = listOf(
            "fight ${robot1.id} ${robot5.id} ${UUID.randomUUID()}",
            "fight ${robot2.id} ${robot5.id} ${UUID.randomUUID()}",
            "fight ${robot5.id} ${robot1.id} ${UUID.randomUUID()}"
        )
        commandDto = CommandDTO(fightCommands)
        for (i in 1..5)
            mockMvc.post("/commands") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(commandDto)
            }
        // robot 1 and 5 should be dead now

        // 4. Retrieve and check robot status
        val player1Robots: List<RobotDto> = mapper.readValue(
            mockMvc.get("/robots?player-id=$player1") {
                contentType = MediaType.APPLICATION_JSON
            }.andReturn().response.contentAsString
        )
        val player2Robots: List<RobotDto> = mapper.readValue(
            mockMvc.get("/robots?player-id=$player2") {
                contentType = MediaType.APPLICATION_JSON
            }.andReturn().response.contentAsString
        )

        assert(player1Robots.map { it.id }.containsAll(listOf(robot2.id, robot3.id)))
        assert(player2Robots.map { it.id }.isEmpty())

        // ///////////////////////////////////// Thrown Events  ////////////////////////////////////////
        consumerRecords.forEach {
            println(it.topic() + ": " + it.value())
        }

        assertEquals(48, consumerRecords.size)
        assertEquals(3, consumerRecords.filter { it.topic() == topicConfig.ROBOT_DESTROYED }.size)
    }

    @Test
    fun `Upgraded robots mining resources and fighting over them with items, resources get dropped`() {
        startItemFightingContainer()
        startFightingContainer()
        startResourceDistributionContainer()
        startMiningContainer()
        startRobotDestroyedContainer()

        consumerRecords.clear()

        // player1, all robots on planet1
        var robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player1, planet1)
        val robot1 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player1, planet1)
        val robot2 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player1, planet1)
        val robot3 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        // player2, robot on on planet1
        robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player2, planet1)
        val robot4 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        val robots = listOf(robot1, robot2, robot3, robot4)

        // ///////////////////////////////////////    Upgrading   /////////////////////////////////////////////
        // Robot1: MiningSpeed Level 2
        // Robot2: MiningSpeed Level 1
        // Robot3: Damage Level 2
        // Robot4: Health Level 1

        mockMvc.post("/robots/${robot1.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "MINING_SPEED",
                "targetLevel": 1
            }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        mockMvc.post("/robots/${robot1.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "MINING_SPEED",
                "targetLevel": 2
            }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        mockMvc.post("/robots/${robot2.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "MINING_SPEED",
                "targetLevel": 1
            }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        mockMvc.post("/robots/${robot3.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "DAMAGE",
                "targetLevel": 1
            }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        mockMvc.post("/robots/${robot3.id}/upgrades") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "DAMAGE",
                "targetLevel": 2
            }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        robots.forEach {
            mockMvc.post("/robots/${it.id}/upgrades") {
                contentType = MediaType.APPLICATION_JSON
                content = """{
                "transactionId": "${UUID.randomUUID()}",
                "upgradeType": "HEALTH",
                "targetLevel": 1
            }"""
            }.andExpect { status { HttpStatus.OK } }.andReturn()
        }

        assertEquals(10, robotRepo.findByIdOrNull(robot1.id)!!.miningSpeed)
        assertEquals(5, robotRepo.findByIdOrNull(robot2.id)!!.miningSpeed)
        assertEquals(5, robotRepo.findByIdOrNull(robot3.id)!!.attackDamage)
        assertEquals(25, robotRepo.findByIdOrNull(robot4.id)!!.health)

        // /////////////////////////////////////////   Mining   ///////////////////////////////////////////////
        println("Mining on planet: ${robot1.planet}")

        val miningCommands = listOf(
            "mine ${robot1.id} ${UUID.randomUUID()}",
            "mine ${robot2.id} ${UUID.randomUUID()}",
            "mine ${robot3.id} ${UUID.randomUUID()}",
            "mine ${robot4.id} ${UUID.randomUUID()}"
        )

        val planet1GameMapDto = GameMapPlanetDto(planet1, 3, resource = ResourceDto(ResourceType.COAL))
        val miningResponse = MineResponseDto(19) // 10 + 5 + 2 + 2
        for (i in 1..3) {
            mockGameServiceWebClient.enqueue(
                MockResponse().setResponseCode(200)
                    .setBody(jacksonObjectMapper().writeValueAsString(planet1GameMapDto))
                    .setHeader("Content-Type", "application/json")
            )
            mockGameServiceWebClient.enqueue(
                MockResponse().setResponseCode(200)
                    .setBody(jacksonObjectMapper().writeValueAsString(miningResponse))
                    .setHeader("Content-Type", "application/json")
            )
        }

        // Last Mining returns less resources than requested
        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(planet1GameMapDto))
                .setHeader("Content-Type", "application/json")
        )
        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(MineResponseDto(10)))
                .setHeader("Content-Type", "application/json")
        )

        for (i in 1..4)
            mockMvc.post("/commands") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(CommandDTO(miningCommands))
            }.andExpect { status { HttpStatus.OK } }.andReturn()

        robots.forEach {
            println("Robot (${it.id}) resources: ")
            val robot = robotRepo.findByIdOrNull(it.id)!!
            ResourceType.values().forEach {
                println("\t$it: " + robot.inventory.getStorageUsageForResource(it))
            }
        }

        // ////////////////////////////////////////  Getting Items  ///////////////////////////////////////////
        mockMvc.post("/robots/${robot1.id}/inventory/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "transactionId": "${UUID.randomUUID()}",
                "itemType": "ROCKET"
            }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        mockMvc.post("/robots/${robot2.id}/inventory/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "transactionId": "${UUID.randomUUID()}",
                "itemType": "ROCKET"
            }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        mockMvc.post("/robots/${robot3.id}/inventory/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
            "transactionId": "${UUID.randomUUID()}",
            "itemType": "LONG_RANGE_BOMBARDMENT"
        }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        mockMvc.post("/robots/${robot4.id}/inventory/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
            "transactionId": "${UUID.randomUUID()}",
            "itemType": "ROCKET"
        }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        assertEquals(1, robotRepo.findByIdOrNull(robot1.id)!!.inventory.getItemAmountByType(AttackItemType.ROCKET))
        assertEquals(1, robotRepo.findByIdOrNull(robot2.id)!!.inventory.getItemAmountByType(AttackItemType.ROCKET))
        assertEquals(1, robotRepo.findByIdOrNull(robot3.id)!!.inventory.getItemAmountByType(AttackItemType.LONG_RANGE_BOMBARDMENT))
        assertEquals(1, robotRepo.findByIdOrNull(robot4.id)!!.inventory.getItemAmountByType(AttackItemType.ROCKET))

        // //////////////////////////////////////////  Item Fighting   /////////////////////////////////////////////
        val attackCommands = listOf(
            "use-item-fighting ${robot1.id} ROCKET ${robot4.id} ${UUID.randomUUID()}",
            "use-item-fighting ${robot2.id} ROCKET ${robot4.id} ${UUID.randomUUID()}",
            "use-item-fighting ${robot3.id} LONG_RANGE_BOMBARDMENT $planet1 ${UUID.randomUUID()}",
            "use-item-fighting ${robot4.id} ROCKET ${robot4.id} ${UUID.randomUUID()}"
        )
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(attackCommands))
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        // Robot4 died, so lost 7 resources from its inventory
        assertEquals(null, robotRepo.findByIdOrNull(robot4.id))
        // +0
        assertEquals(20, robotRepo.findByIdOrNull(robot1.id)!!.inventory.getStorageUsageForResource(ResourceType.COAL))
        // +2
        assertEquals(20, robotRepo.findByIdOrNull(robot2.id)!!.inventory.getStorageUsageForResource(ResourceType.COAL))
        // +5
        assertEquals(12, robotRepo.findByIdOrNull(robot3.id)!!.inventory.getStorageUsageForResource(ResourceType.COAL))

        // //////////////////////////////////////// Thrown Events  //////////////////////////////////////////////////
        consumerRecords.forEach {
            println(it.topic() + ": " + it.value())
        }

        /*
            16 Mining Events (4 * 4 Mine Commands)
            7 Fighting Events ( 3 * Rocket + 4 Long Range Bombardment)
            4 Item Fighting Events
            3 Resource Distribution Events ( 3 remaining robots on planet)
            = 30
         */
        assertEquals(31, consumerRecords.size)
        assertEquals(1, consumerRecords.filter { it.topic() == topicConfig.ROBOT_DESTROYED }.size)
    }

    @Test
    fun `Robots block, fight, then regenerate, repair or flee with movement-item`() {
        startFightingContainer()
        startPlanetBlockedContainer()
        startItemMovementContainer()
        startMovementContainer()
        startRegenerationContainer()
        startItemRepairContainer()

        consumerRecords.clear()

        var robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player1, planet1)
        val robot1 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player2, planet1)
        val robot2 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        robotSpawnDto = RobotSpawnDto(UUID.randomUUID(), player2, planet1)
        val robot3 = mapper.readValue(
            mockMvc.post("/robots") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(robotSpawnDto)
            }.andReturn().response.contentAsString,
            RobotDto::class.java
        )

        // ///////////////////////////////////////// Blocking ////////////////////////////////////////////////
        val blockCommand = "block ${robot1.id} ${UUID.randomUUID()}"
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(blockCommand)))
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        assertEquals(planet1, planetRepo.findAllByBlocked(true).first().planetId)

        // ///////////////////////////////////////// Fighting ////////////////////////////////////////////////
        val fightCommands = listOf(
            "fight ${robot1.id} ${robot2.id} ${UUID.randomUUID()}",
            "fight ${robot2.id} ${robot3.id} ${UUID.randomUUID()}",
            "fight ${robot3.id} ${robot1.id} ${UUID.randomUUID()}"
        )
        for (i in 1..3)
            mockMvc.post("/commands") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(CommandDTO(fightCommands))
            }.andExpect { status { HttpStatus.OK } }.andReturn()

        assertEquals(7, robotRepo.findByIdOrNull(robot1.id)!!.health)
        assertEquals(20 - 4 - 3, robotRepo.findByIdOrNull(robot1.id)!!.energy) // blocking + fighting
        assertEquals(7, robotRepo.findByIdOrNull(robot2.id)!!.health)
        assertEquals(20 - 3, robotRepo.findByIdOrNull(robot2.id)!!.energy)
        assertEquals(7, robotRepo.findByIdOrNull(robot3.id)!!.health)
        assertEquals(20 - 3, robotRepo.findByIdOrNull(robot3.id)!!.energy)

        // /////////////////////////////////// Repairing + Regenerating ////////////////////////////////////////
        mockMvc.post("/robots/${robot2.id}/inventory/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
            "transactionId": "${UUID.randomUUID()}",
            "itemType": "REPAIR_SWARM"
        }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        val regenCommand = listOf(
            "regenerate ${robot1.id} ${UUID.randomUUID()}",
        )
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(regenCommand))
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        val repairCommand = listOf(
            "use-item-repair ${robot2.id} REPAIR_SWARM ${UUID.randomUUID()}"
        )
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(repairCommand))
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        assertEquals(13 + upgradeValues.energyRegenerationValues.getByVal(0), robotRepo.findByIdOrNull(robot1.id)!!.energy)
        assertEquals(10, robotRepo.findByIdOrNull(robot2.id)!!.health)
        assertEquals(10, robotRepo.findByIdOrNull(robot3.id)!!.health)

        // ////////////////////////////////// Wormhole ///////////////////////////////////////////
        mockMvc.post("/robots/${robot1.id}/inventory/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
            "transactionId": "${UUID.randomUUID()}",
            "itemType": "WORMHOLE"
        }"""
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        val wormholeCommand = listOf(
            "use-item-movement ${robot1.id} WORMHOLE ${UUID.randomUUID()}"
        )
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(wormholeCommand))
        }.andExpect { status { HttpStatus.OK } }.andReturn()

        // Movement doesnt work because planet is blocked
        assertEquals(planet1, robotRepo.findByIdOrNull(robot1.id)!!.planet.planetId)

        // /////////////////////////////////// Events ////////////////////////////////////
        consumerRecords.forEach {
            println(it.topic() + ": " + it.value())
        }

        /*
        9 fight events
        1 block event
        1 repair item event
        1 movement event
        1 item movement event
        1 regenerate event
         */
        // assertEquals(14, consumerRecords.size)
    }
}

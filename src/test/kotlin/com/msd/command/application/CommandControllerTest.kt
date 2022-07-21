package com.msd.command.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.AbstractKafkaProducerTest
import com.msd.application.dto.GameMapNeighbourDto
import com.msd.application.dto.GameMapPlanetDto
import com.msd.application.dto.MineResponseDto
import com.msd.application.dto.ResourceDto
import com.msd.domain.DomainEvent
import com.msd.domain.ResourceType
import com.msd.event.application.EventType
import com.msd.event.application.ProducerTopicConfiguration
import com.msd.event.application.dto.*
/*import com.msd.item.domain.AttackItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.RepairItemType
*/

import com.msd.planet.application.PlanetDTO
import com.msd.planet.domain.MapDirection
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetType
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.UpgradeType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://\${spring.kafka.bootstrap-servers}", "port=9092"]
)
@ActiveProfiles(profiles = ["no-async", "test"])
@Transactional
class CommandControllerTest(
    @Autowired private var mockMvc: MockMvc,
    @Autowired private var commandController: CommandController,
    @Autowired private var robotRepository: RobotRepository,
    @Autowired private var mapper: ObjectMapper,
    @Autowired private val embeddedKafka: EmbeddedKafkaBroker,
    @Autowired private val topicConfig: ProducerTopicConfiguration
) : AbstractKafkaProducerTest(embeddedKafka, topicConfig) {
    private val player1Id = UUID.randomUUID()
    private val player2Id = UUID.randomUUID()

    private val planet1Id = UUID.randomUUID()
    private val planet2Id = UUID.randomUUID()

    private lateinit var robot1: Robot
    private lateinit var robot2: Robot
    private lateinit var robot3: Robot
    private lateinit var robot4: Robot
    private lateinit var robot5: Robot
    private lateinit var robot6: Robot
    private lateinit var robot7: Robot
    private lateinit var robot8: Robot
    private lateinit var robots: List<Robot>

    private val mockGameServiceWebClient = MockWebServer()

    @BeforeEach
    fun `setup database`() {
        robot1 = robotRepository.save(Robot(player1Id, Planet(planet1Id)))
        robot2 = robotRepository.save(Robot(player1Id, Planet(planet1Id)))
        robot3 = robotRepository.save(Robot(player1Id, Planet(planet2Id)))
        robot4 = robotRepository.save(Robot(player1Id, Planet(planet2Id)))
        robot5 = robotRepository.save(Robot(player2Id, Planet(planet1Id)))
        robot6 = robotRepository.save(Robot(player2Id, Planet(planet1Id)))
        robot7 = robotRepository.save(Robot(player2Id, Planet(planet2Id)))
        robot8 = robotRepository.save(Robot(player2Id, Planet(planet2Id)))
        robots = listOf(robot1, robot2, robot3, robot4, robot5, robot6, robot7, robot8)

        consumerRecords = LinkedBlockingQueue()

        mockGameServiceWebClient.start(port = 8081)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        mockGameServiceWebClient.shutdown()
    }

    @Test
    fun `application context loads`() {
        assertNotNull(commandController)
        assertNotNull(mockMvc)
        assertNotNull(robotRepository)
        assertNotNull(mapper)
    }

    @Test
    fun `incorrect commands return error code 400`() {
        // given
        val command1 = "regenerate ${UUID.randomUUID()}"
        val command2 = "block ${robot2.id}"
        val command3 = "move ${robot2.id}"
        val command4 = "mine ${robot2.id} broken"
        val command5 = "use-item-movement ${robot2.id} broken ${UUID.randomUUID()}"
        val command6 = "fight ${robot6.id} noTarget ${UUID.randomUUID()}"
        val command7 = "use-item-fighting ${robot2.id} broken ${UUID.randomUUID()}"
        val command8 = "nonsense ${UUID.randomUUID()}"
        val commands = listOf(command1, command2, command3, command4, command5, command6, command7, command8)

        // then
        commands.forEach {
            mockMvc.post("/commands") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(CommandDTO(listOf(it)))
            }.andExpect {
                status { isBadRequest() }
            }.andDo { print() }
        }
    }

    @Test
    fun `fighting works correctly`() {
        // given
        startFightingContainer()
        consumerRecords.clear()

        val command1 = "fight ${robot1.id} ${robot5.id} ${UUID.randomUUID()}"
        val command2 = "fight ${robot2.id} ${robot6.id} ${UUID.randomUUID()}"
        val command3 = "fight ${robot3.id} ${robot7.id} ${UUID.randomUUID()}"
        val command4 = "fight ${robot4.id} ${robot8.id} ${UUID.randomUUID()}"
        val command5 = "fight ${robot5.id} ${robot1.id} ${UUID.randomUUID()}"
        val command6 = "fight ${robot6.id} ${robot2.id} ${UUID.randomUUID()}"
        val command7 = "fight ${robot7.id} ${robot3.id} ${UUID.randomUUID()}"
        val command8 = "fight ${robot8.id} ${robot4.id} ${UUID.randomUUID()}"

        val commands = listOf(command1, command2, command3, command4, command5, command6, command7, command8)

        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(commands))
        }.andExpect {
            status { isAccepted() }
            content { string("Command batch accepted") }
        }.andDo { print() }

        commands.forEach {
            println(it.split(" ").last())
        }
        println()
        consumerRecords.forEach {
            println(it.topic() + " " + it.headers().toArray().forEach { println("\t" + it.key() + ": " + String(it.value())) })
        }
        // then
        assertAll(
            "Check all robot values",
            robots.map {
                {
                    assertEquals(9, robotRepository.findByIdOrNull(it.id)!!.health)
                    assertEquals(19, robotRepository.findByIdOrNull(it.id)!!.energy)
                }
            }
        )

        assertAll(
            commands.map {
                {
                    val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
                    assertNotNull(singleRecord!!)
                    assertEquals(topicConfig.ROBOT_FIGHTING, singleRecord.topic())
                    val domainEvent = DomainEvent.build(
                        jacksonObjectMapper().readValue(singleRecord.value(), FightingEventDTO::class.java),
                        singleRecord.headers()
                    )
                    eventTestUtils.checkHeaders(UUID.fromString(it.split(" ").last()), EventType.FIGHTING, domainEvent)
                    eventTestUtils.checkFightingPayload(
                        true,
                        "Attacking successful",
                        UUID.fromString(it.split(" ")[1]),
                        UUID.fromString(it.split(" ")[2]),
                        9,
                        19,
                        domainEvent.payload
                    )
                }
            }
        )
    }

    @Test
    fun `destroyed robots get deleted after combat attacks are executed`() {
        startFightingContainer()
        startRobotDestroyedContainer()
        // given
        for (i in 1..3) robot1.upgrade(UpgradeType.DAMAGE, i)
        assertEquals(10, robot1.attackDamage)
        robotRepository.save(robot1)
        val command = "fight ${robot1.id} ${robot5.id} ${UUID.randomUUID()}"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isAccepted() }
            content { string("Command batch accepted") }
        }.andDo { print() }
        // then
        assertNull(robotRepository.findByIdOrNull(robot5.id))
        assertEquals(1, consumerRecords.filter { it.topic() == topicConfig.ROBOT_FIGHTING }.size)
        assertEquals(1, consumerRecords.filter { it.topic() == topicConfig.ROBOT_DESTROYED }.size)
    }

    @Test
    fun `movement works correctly`() {
        // given
        startMovementContainer()

        // robot1's current planet is the neighbour of the target planet
        val targetPlanetDto = GameMapPlanetDto(
            planet2Id, 3,
            neighbours = listOf(GameMapNeighbourDto(robot1.planet.planetId, 3, MapDirection.NORTH))
        )

        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(targetPlanetDto))
        )

        val command = "move ${robot1.id} $planet2Id ${UUID.randomUUID()}"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isAccepted() }
            content { string("Command batch accepted") }
        }.andDo { print() }

        // then
        val robot = robotRepository.findByIdOrNull(robot1.id)!!
        assertEquals(planet2Id, robot.planet.planetId)
        assertEquals(17, robot.energy)

        // events
        val domainEvent = eventTestUtils.getNextEventOfTopic<MovementEventDTO>(consumerRecords, topicConfig.ROBOT_MOVEMENT)
        eventTestUtils.checkHeaders(UUID.fromString(command.split(" ").last()), EventType.MOVEMENT, domainEvent)
        eventTestUtils.checkMovementPayload(
            true,
            "Movement successful",
            17,
            PlanetDTO(targetPlanetDto.id, targetPlanetDto.movementDifficulty, PlanetType.DEFAULT, null),
            listOf(robot1.id, robot3.id, robot4.id, robot7.id, robot8.id),
            domainEvent.payload
        )

        // TODO Neighbors Event checken
    }

   /* @Test
    fun `block works correctly`() {
        // given
        startPlanetBlockedContainer()

        val transactionId = UUID.randomUUID()
        val command = "block ${robot1.id} $transactionId"

        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isAccepted() }
            content { string("Command batch accepted") }
        }.andDo { print() }
        // then
        assertAll(
            "All robots on planet1 have planet block",
            robotRepository.findAllByPlanet_PlanetId(planet1Id).map {
                {
                    assert(it.planet.blocked)
                }
            }
        )
        assertEquals(16, robotRepository.findByIdOrNull(robot1.id)!!.energy)

        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_BLOCKED, singleRecord.topic())
        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), BlockEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(transactionId, EventType.PLANET_BLOCKED, domainEvent)
        eventTestUtils.checkBlockPayload(
            true,
            "Planet with ID: $planet1Id has been blocked",
            planet1Id,
            16,
            domainEvent.payload
        )
    }
*/
    /*
    @Test
    fun `robots can't move from blocked planet`() {
        // given
        startMovementContainer()
        consumerRecords.clear()

        val moveCommandId = UUID.randomUUID()

        // robot1s current planet is the neighbour of the target planet
        val targetPlanetDto = GameMapPlanetDto(
            planet2Id, 3,
            neighbours = listOf(GameMapNeighbourDto(robot1.planet.planetId, 3, MapDirection.NORTH))
        )
        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(targetPlanetDto))
        )

        val command1 = "block ${robot1.id} ${UUID.randomUUID()}"
        val command2 = "move ${robot2.id} $planet2Id $moveCommandId"
        val commands = listOf(command1, command2)

        // when
        commands.forEach {
            mockMvc.post("/commands") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(CommandDTO(listOf(it)))
            }.andExpect {
                status { isAccepted() }
                content { string("Command batch accepted") }
            }.andDo { print() }
        }

        // then
        assertEquals(planet1Id, robotRepository.findByIdOrNull(robot2.id)!!.planet.planetId)
        assertEquals(17, robotRepository.findByIdOrNull(robot2.id)!!.energy)
        // events
        val domainEvent = eventTestUtils.getNextEventOfTopic<MovementEventDTO>(consumerRecords, topicConfig.ROBOT_MOVEMENT)
        eventTestUtils.checkHeaders(moveCommandId, EventType.MOVEMENT, domainEvent)
        eventTestUtils.checkMovementPayload(
            false,
            "Can't move out of a blocked planet",
            17,
            null,
            listOf(),
            domainEvent.payload
        )
    }
*/
    @Test
    fun `robot correctly regenerates energy`() {
        // given
        startRegenerationContainer()
        consumerRecords.clear()

        robot1.move(Planet(planet2Id), 10)
        assertEquals(10, robot1.energy)
        robotRepository.save(robot1)

        val transactionId = UUID.randomUUID()
        val command = "regenerate ${robot1.id} $transactionId"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isAccepted() }
            content { string("Command batch accepted") }
        }.andDo { print() }
        // then
        assertEquals(14, robotRepository.findByIdOrNull(robot1.id)!!.energy)

        // events
        println("Consumer size: ${consumerRecords.size}")
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_REGENERATION, singleRecord.topic())
        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), RegenerationEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(transactionId, EventType.REGENERATION, domainEvent)
        eventTestUtils.checkRegenerationPayload(
            true,
            "Robot regenerated 4 energy",
            14,
            domainEvent.payload,
        )
    }

  /*  @Test
    fun `all robots correctly regenerate health when using repair swarm`() {
        // given
        startItemRepairContainer()

        val transactionId = UUID.randomUUID()
        val command = "use-item-repair ${robot1.id} ${RepairItemType.REPAIR_SWARM} $transactionId"
        robot1.upgrade(UpgradeType.HEALTH, 1)
        robot2.upgrade(UpgradeType.HEALTH, 1)
        robot1.receiveDamage(21)
        robot2.receiveDamage(10)
        robot1.inventory.addItem(RepairItemType.REPAIR_SWARM)
        robotRepository.saveAll(listOf(robot1, robot2))
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isAccepted() }
            content { string("Command batch accepted") }
        }.andDo { print() }

        // then

        val robot1rep = robotRepository.findByIdOrNull(robot1.id)!!
        val robot2rep = robotRepository.findByIdOrNull(robot2.id)!!
        assertEquals(0, robot1rep.inventory.getItemAmountByType(RepairItemType.REPAIR_SWARM))
        assertAll(
            "assert all robots healed correctly",
            {
                assertEquals(24, robot1rep.health)
            },
            {
                assertEquals(25, robot2rep.health)
            }
        )

        // events
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_REPAIR, singleRecord.topic())
        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemRepairEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(transactionId, EventType.ITEM_REPAIR, domainEvent)
        eventTestUtils.checkItemRepairPayload(
            true,
            "Robot has used ${RepairItemType.REPAIR_SWARM}",
            listOf(robot1rep, robot2rep).map { RepairEventRobotDTO(it.id, it.health) },
            domainEvent.payload,
        )
    }

    @Test
    fun `robot moves to a random planet after using a wormhole`() {
        // given
        startItemMovementContainer()
        startMovementContainer()

        val transactionId = UUID.randomUUID()
        val command = "use-item-movement ${robot1.id} ${MovementItemType.WORMHOLE} $transactionId"
        robot1.inventory.addItem(MovementItemType.WORMHOLE)
        robotRepository.save(robot1)

        val planetDto1 = GameMapPlanetDto(UUID.randomUUID(), 3)
        val planetDto2 = GameMapPlanetDto(UUID.randomUUID(), 3)
        val planetDto3 = GameMapPlanetDto(UUID.randomUUID(), 3)
        val planetDto4 = GameMapPlanetDto(UUID.randomUUID(), 3)
        val planetDTOs = listOf(planetDto1, planetDto2, planetDto3, planetDto4)

        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(200).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(jacksonObjectMapper().writeValueAsString(planetDTOs))
        )
        // then
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isAccepted() }
            content { string("Command batch accepted") }
        }.andDo { print() }

        // then
        robot1 = robotRepository.findByIdOrNull(robot1.id)!!
        assertNotEquals(planet1Id, robot1.planet.planetId)
        assertEquals(0, robot1.inventory.getItemAmountByType(MovementItemType.WORMHOLE))

        // events
        // movement
        val domainEventMovement = eventTestUtils.getNextEventOfTopic<MovementEventDTO>(consumerRecords, topicConfig.ROBOT_MOVEMENT)

        eventTestUtils.checkHeaders(transactionId, EventType.MOVEMENT, domainEventMovement)
        eventTestUtils.checkMovementPayload(
            true,
            "Movement successful",
            20,
            PlanetDTO(robot1.planet.planetId, 3, PlanetType.DEFAULT, null),
            listOf(robot1.id),
            domainEventMovement.payload,
        )

        // item-movement
        val domainEventItem = eventTestUtils.getNextEventOfTopic<ItemMovementEventDTO>(consumerRecords, topicConfig.ROBOT_ITEM_MOVEMENT)

        val movementEventId = domainEventMovement.id

        eventTestUtils.checkHeaders(transactionId, EventType.ITEM_MOVEMENT, domainEventItem)
        eventTestUtils.checkItemMovementPayload(
            true,
            "Item usage successful",
            UUID.fromString(movementEventId),
            domainEventItem.payload,
        )
    }
*/
    @Test
    fun `Sending EnergyRegen Command with invalid robot UUID returns 400`() {
        val command = "regenerate invalidRobotId ${UUID.randomUUID()}"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `Sending EnergyRegen Command with invalid transaction UUID returns 400 and does not increase the robots energy`() {

        robot1.move(Planet(UUID.randomUUID()), 10)
        assertEquals(10, robot1.energy)
        robotRepository.save(robot1)

        val command = "regenerate ${UUID.randomUUID()} invalidTransactionId"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isBadRequest() }
        }

        assertEquals(10, robotRepository.findByIdOrNull(robot1.id)!!.energy)
    }

    @Test
    fun `Sending an EnergyRegen command doesnt lead to a robot's energy being increased past max Energy amount`() {
        // given
        robot1.move(Planet(UUID.randomUUID()), 1)
        assertEquals(19, robot1.energy)
        robotRepository.save(robot1)

        val command = "regenerate ${robot1.id} ${UUID.randomUUID()}"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isAccepted() }
        }
        // then
        assertEquals(20, robotRepository.findByIdOrNull(robot1.id)!!.energy)
    }
/*
    @Test
    fun `Robot cannot use RepairSwarm if it doesn't have the item`() {
        startItemRepairContainer()
        // given
        val robotsOnPlanet1Player1 = listOf(robot1, robot2)
        robotsOnPlanet1Player1.forEach {
            it.upgrade(UpgradeType.HEALTH, 1)
            it.receiveDamage(21)
        }
        robotRepository.saveAll(robotsOnPlanet1Player1)

        val transactionId = UUID.randomUUID()
        val command = "use-item-repair ${robot1.id} ${RepairItemType.REPAIR_SWARM} $transactionId"

        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isAccepted() }
        }
        // then
        assertEquals(
            0,
            robotRepository.findByIdOrNull(robot1.id)!!.inventory.getItemAmountByType(RepairItemType.REPAIR_SWARM)
        )
        assertAll(
            robotsOnPlanet1Player1.map {
                {
                    assertEquals(4, robotRepository.findByIdOrNull(it.id)!!.health)
                }
            }
        )

        val domainEvent =
            eventTestUtils.getNextEventOfTopic<ItemRepairEventDTO>(consumerRecords, topicConfig.ROBOT_ITEM_REPAIR)

        eventTestUtils.checkHeaders(transactionId, EventType.ITEM_REPAIR, domainEvent)
        eventTestUtils.checkItemRepairPayload(
            false,
            "This Robot doesn't have the required Item\nMissing item: ${RepairItemType.REPAIR_SWARM}",
            listOf(),
            domainEvent.payload
        )
    }
*/
    @Test
    fun `Sending Any heterogeneous command lists causes 400`() {
        // given
        val heterogeneousMineCommand = listOf(
            "mine ${robot1.id} ${UUID.randomUUID()}",
            "block ${robot2.id} ${UUID.randomUUID()}"
        )

        val heterogeneousFightCommand = listOf(
            "fight ${robot1.id} ${robot2.id} ${UUID.randomUUID()}",
            "block ${robot2.id} ${UUID.randomUUID()}"
        )

        val heterogeneousBlockCommand = listOf(
            "block ${robot1.id} ${UUID.randomUUID()}",
            "mine ${robot2.id} ${UUID.randomUUID()}"
        )

        val heterogeneousMoveCommand = listOf(
            "move ${robot1.id} $planet1Id ${UUID.randomUUID()}",
            "block ${robot2.id} ${UUID.randomUUID()}"
        )

        val heterogeneousEnergyRegenCommand = listOf(
            "regenerate ${robot1.id} ${UUID.randomUUID()}",
            "block ${robot2.id} ${UUID.randomUUID()}"
        )

      /*  val heterogeneousItemFightingCommand = listOf(
            "use-item-fighting ${robot1.id} ${AttackItemType.ROCKET} ${robot2.id} ${UUID.randomUUID()}",
            "block ${robot2.id} ${UUID.randomUUID()}"
        )

        val heterogeneousItemMoveCommand = listOf(
            "use-item-movement ${robot1.id} ${MovementItemType.WORMHOLE} ${UUID.randomUUID()}",
            "block ${robot2.id} ${UUID.randomUUID()}"
        )

        val heterogeneousItemRepairCommand = listOf(
            "use-item-repair ${robot1.id} ${RepairItemType.REPAIR_SWARM} ${UUID.randomUUID()}",
            "block ${robot2.id} ${UUID.randomUUID()}"
        )
*/
        val commandBatches = listOf(
            heterogeneousMineCommand,
            heterogeneousFightCommand,
            heterogeneousBlockCommand,
       //     heterogeneousItemMoveCommand,
            heterogeneousEnergyRegenCommand,
        //    heterogeneousItemFightingCommand,
        //    heterogeneousItemRepairCommand,
            heterogeneousMoveCommand
        )

        // when
        commandBatches.forEach {
            mockMvc.post("/commands") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(CommandDTO(it))
            }.andExpect {
                // then
                status { isBadRequest() }
                content { string("Command batches need to be homogeneous.") }
            }
        }
    }

    @Test
    fun `Planet unknown to MapService causes stops corresponding mining command`() {
        startMiningContainer()

        val transId1 = UUID.randomUUID()
        val transId2 = UUID.randomUUID()

        val mineCommands = listOf(
            "mine ${robot1.id} $transId1", // returns planet
            "mine ${robot3.id} $transId2" // returns unknown planet
        )

        val planet1GameMapDto = GameMapPlanetDto(
            planet1Id,
            3,
            false,
            ResourceDto(ResourceType.COAL)
        )

        val miningResponse = MineResponseDto(2)

        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(planet1GameMapDto))
                .setHeader("Content-Type", "application/json")
        )
        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(400)
                .setHeader("Content-Type", "application/json")
        )
        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(201)
                .setBody(jacksonObjectMapper().writeValueAsString(miningResponse))
                .setHeader("Content-Type", "application/json")
        )

        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(mineCommands))
        }.andExpect {
            // then
            status { isAccepted() }
        }

        assertEquals(
            2,
            robotRepository.findByIdOrNull(robot1.id)!!.inventory.getStorageUsageForResource(ResourceType.COAL)
        )
        assertEquals(
            0,
            robotRepository.findByIdOrNull(robot3.id)!!.inventory.getStorageUsageForResource(ResourceType.COAL)
        )

        val domainEvent1 = eventTestUtils.getNextEventOfTopic<MiningEventDTO>(consumerRecords, topicConfig.ROBOT_MINING)

        val domainEvent2 = eventTestUtils.getNextEventOfTopic<MiningEventDTO>(consumerRecords, topicConfig.ROBOT_MINING)

        eventTestUtils.checkHeaders(transId1, EventType.MINING, domainEvent2)
        eventTestUtils.checkMiningPayload(
            true,
            "Robot ${robot1.id} mined successfully",
            20,
            2,
            ResourceType.COAL.toString(),
            domainEvent2.payload
        )

        eventTestUtils.checkHeaders(transId2, EventType.MINING, domainEvent1)
        eventTestUtils.checkMiningPayload(
            false,
            "Map Service did not return any resource on the planet $planet2Id",
            20,
            0,
            "none",
            domainEvent1.payload
        )
    }

    @Test
    fun `Planet without resource stops corresponding mining command`() {
        startMiningContainer()

        val transId1 = UUID.randomUUID()
        val transId2 = UUID.randomUUID()

        val mineCommands = listOf(
            "mine ${robot1.id} $transId1", // returns resource
            "mine ${robot3.id} $transId2" // returns no resource
        )

        val planet1GameMapDto = GameMapPlanetDto(
            planet1Id,
            3,
            false,
            ResourceDto(ResourceType.COAL)
        )

        val planet2GameMapDto = GameMapPlanetDto(
            planet1Id,
            3,
            false
        )

        val miningResponse = MineResponseDto(2)

        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(planet1GameMapDto))
                .setHeader("Content-Type", "application/json")
        )
        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(planet2GameMapDto))
                .setHeader("Content-Type", "application/json")
        )
        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(201)
                .setBody(jacksonObjectMapper().writeValueAsString(miningResponse))
                .setHeader("Content-Type", "application/json")
        )

        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(mineCommands))
        }.andExpect {
            // then
            status { isAccepted() }
        }

        assertEquals(
            2,
            robotRepository.findByIdOrNull(robot1.id)!!.inventory.getStorageUsageForResource(ResourceType.COAL)
        )
        assertEquals(
            0,
            robotRepository.findByIdOrNull(robot3.id)!!.inventory.getStorageUsageForResource(ResourceType.COAL)
        )

        val domainEvent1 = eventTestUtils.getNextEventOfTopic<MiningEventDTO>(consumerRecords, topicConfig.ROBOT_MINING)

        val domainEvent2 = eventTestUtils.getNextEventOfTopic<MiningEventDTO>(consumerRecords, topicConfig.ROBOT_MINING)

        eventTestUtils.checkHeaders(transId1, EventType.MINING, domainEvent2)
        eventTestUtils.checkMiningPayload(
            true,
            "Robot ${robot1.id} mined successfully",
            20,
            2,
            "coal",
            domainEvent2.payload
        )

        eventTestUtils.checkHeaders(transId2, EventType.MINING, domainEvent1)
        eventTestUtils.checkMiningPayload(
            false,
            "Map Service did not return any resource on the planet $planet2Id",
            20,
            0,
            "none",
            domainEvent1.payload
        )
    }

    @Test
    fun `Unreachable MapService causes event to be thrown during mining`() {
        // given
        startMiningContainer()

        val planet1GameMapDto = GameMapPlanetDto(
            planet1Id,
            3,
            false,
            ResourceDto(ResourceType.COAL)
        )

        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(planet1GameMapDto))
                .setHeader("Content-Type", "application/json")
        )

        val transactionId = UUID.randomUUID()
        val command = listOf("mine ${robot1.id} $transactionId")

        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(command))
        }.andExpect {
            // then
            status { isAccepted() }
        }

        // then
        val domainEvent = eventTestUtils.getNextEventOfTopic<MiningEventDTO>(consumerRecords, topicConfig.ROBOT_MINING)
        eventTestUtils.checkHeaders(transactionId, EventType.MINING, domainEvent)
        eventTestUtils.checkMiningPayload(
            false,
            "Unexpected exception occurred: Could not connect to Map Service",
            20,
            0,
            "none",
            domainEvent.payload
        )
    }

  /*  @Test
    fun `Unreachable MapService causes event to be thrown during movement item usage`() {
        // given
        startItemMovementContainer()

        robot1.inventory.addItem(MovementItemType.WORMHOLE)
        robotRepository.save(robot1)

        val transactionId = UUID.randomUUID()
        val command = listOf("use-item-movement ${robot1.id} ${MovementItemType.WORMHOLE} $transactionId")

        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(command))
        }.andExpect {
            // then
            status { isAccepted() }
        }

        // then
        val domainEvent1 = eventTestUtils.getNextEventOfTopic<ItemMovementEventDTO>(consumerRecords, topicConfig.ROBOT_ITEM_MOVEMENT)
        eventTestUtils.checkItemMovementPayload(
            false,
            "Unexpected exception occurred: Could not connect to Map Service",
            null,
            domainEvent1.payload
        )
    }
*/
    @Test
    fun `Unreachable MapService causes event to be thrown during movement`() {
        // given
        startMovementContainer()

        val transactionId = UUID.randomUUID()
        val command = listOf("move ${robot1.id} $planet2Id $transactionId")

        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(command))
        }.andExpect {
            // then
            status { isAccepted() }
        }

        // then
        val domainEvent1 = eventTestUtils.getNextEventOfTopic<MovementEventDTO>(consumerRecords, topicConfig.ROBOT_MOVEMENT)
        eventTestUtils.checkMovementPayload(
            false,
            "Unexpected exception occurred: Could not connect to Map Service",
            20,
            null,
            listOf(),
            domainEvent1.payload
        )
    }
}

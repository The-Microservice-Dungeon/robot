package com.msd.command.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.AbstractKafkaProducerTest
import com.msd.application.dto.GameMapPlanetDto
import com.msd.domain.DomainEvent
import com.msd.event.application.EventType
import com.msd.event.application.ProducerTopicConfiguration
import com.msd.event.application.dto.FightingEventDTO
import com.msd.event.application.dto.MovementEventDTO
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.RepairItemType
import com.msd.planet.application.PlanetDTO
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetType
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.UpgradeType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
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
@ActiveProfiles(profiles = ["no-async"])
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

    companion object {
        val mockGameServiceWebClient = MockWebServer()

        @BeforeAll
        @JvmStatic
        internal fun setUp() {
            mockGameServiceWebClient.start(port = 8080)
        }

        @AfterAll
        @JvmStatic
        internal fun tearDown() {
            mockGameServiceWebClient.shutdown()
        }
    }

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
    }

    @AfterEach
    fun tearDown() {
        shutDownAllContainers()
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
    fun `can't mix attack commands with other commands`() {
        // given
        val command1 = "fight ${robot1.id} ${robot5.id} ${UUID.randomUUID()}"
        val command2 = "regenerate ${robot2.id} ${UUID.randomUUID()}"

        val commands = listOf(command1, command2)

        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(commands))
        }.andExpect {
            status { isBadRequest() }
            content { string("AttackCommands need to be homogeneous.") }
        }.andDo { print() }

        // then
        assertEquals(10, robot5.health)
        assertEquals(20, robot1.energy)
    }

    @Test
    fun `fighting works correctly`() {
        // given
        startFightingContainer()

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

        // clean up
        shutDownAllContainers()
    }

    @Test
    fun `destroyed robots get deleted after combat attacks are executed`() {
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
    }

    @Test
    fun `movement works correctly`() {
        // given
        startMovementContainer()

        val targetPlanetDto = GameMapPlanetDto(planet2Id, 3)

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
        eventTestUtils.checkMovementPaylod(
            true,
            "Movement successful",
            17,
            PlanetDTO(targetPlanetDto.id, targetPlanetDto.movementDifficulty, PlanetType.DEFAULT, null),
            listOf(robot1.id, robot3.id, robot4.id, robot7.id, robot8.id),
            domainEvent.payload
        )

        // TODO Neighbors Event checken

        // clean up
        shutDownAllContainers()
    }

    @Test
    fun `block works correctly`() {
        // given
        val command = "block ${robot1.id} ${UUID.randomUUID()}"

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
    }

    @Test
    fun `robots can't move from blocked planet`() {
        startMovementContainer()
        // given
        val targetPlanetDto = GameMapPlanetDto(planet2Id, 3)

        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(targetPlanetDto))
        )

        val command1 = "block ${robot1.id} ${UUID.randomUUID()}"
        val command2 = "move ${robot2.id} $planet2Id ${UUID.randomUUID()}"
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
        eventTestUtils.checkHeaders(UUID.fromString(commands[1].split(" ").last()), EventType.MOVEMENT, domainEvent)
        eventTestUtils.checkMovementPaylod(
            false,
            "Can't move out of a blocked planet",
            17,
            null,
            listOf(),
            domainEvent.payload
        )

        // clean up
        shutDownAllContainers()
    }

    @Test
    fun `robot correctly regenerates energy`() {
        // given
        robot1.move(Planet(planet2Id), 10)
        assertEquals(10, robot1.energy)
        robotRepository.save(robot1)
        val command = "regenerate ${robot1.id} ${UUID.randomUUID()}"
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
    }

    @Test
    fun `all robots correctly regenerate health when using repair swarm`() {
        // given
        val command = "use-item-repair ${robot1.id} ${RepairItemType.REPAIR_SWARM} ${UUID.randomUUID()}"
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

        assertAll(
            "assert all robots healed correctly",
            {
                assertEquals(24, robotRepository.findByIdOrNull(robot1.id)!!.health)
                assertEquals(0, robotRepository.findByIdOrNull(robot1.id)!!.inventory.getItemAmountByType(RepairItemType.REPAIR_SWARM))
            },
            {
                assertEquals(25, robotRepository.findByIdOrNull(robot2.id)!!.health)
            }
        )
    }

    @Test
    fun `robot moves to a random planet after using a wormhole`() {
        // given
        val command = "use-item-movement ${robot1.id} ${MovementItemType.WORMHOLE} ${UUID.randomUUID()}"
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
        assertNotEquals(planet1Id, robotRepository.findByIdOrNull(robot1.id)!!.planet.planetId)
        assertEquals(0, robotRepository.findByIdOrNull(robot1.id)!!.inventory.getItemAmountByType(MovementItemType.WORMHOLE))
    }

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
}

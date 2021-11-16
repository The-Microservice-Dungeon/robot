package com.msd.command.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.GameMapPlanetDto
import com.msd.planet.domain.Planet
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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["no-async"])
class CommandControllerTest(
    @Autowired private var mockMvc: MockMvc,
    @Autowired private var commandController: CommandController,
    @Autowired private var robotRepository: RobotRepository,
    @Autowired private var mapper: ObjectMapper,
) {
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
        val command1 = "regenerate ${robot2.player} ${UUID.randomUUID()}"
        val command2 = "block ${robot2.player} ${robot2.id}"
        val command3 = "move ${robot2.player} ${robot2.id}"
        val command4 = "mine ${robot2.player} ${robot2.id} broken"
        val command5 = "use-item-movement ${UUID.randomUUID()} ${robot2.id} broken ${UUID.randomUUID()}"
        val command6 = "fight ${robot6.player} ${robot6.id} noTarget ${UUID.randomUUID()}"
        val command7 = "use-item-fighting ${UUID.randomUUID()} ${robot2.id} broken ${UUID.randomUUID()}"
        val command8 = "nonsense ${UUID.randomUUID()} ${UUID.randomUUID()} ${UUID.randomUUID()}"
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
        val command1 = "fight ${robot1.player} ${robot1.id} ${robot5.id} ${UUID.randomUUID()}"
        val command2 = "regenerate ${robot2.player} ${robot2.id} ${UUID.randomUUID()}"

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
        val command1 = "fight ${robot1.player} ${robot1.id} ${robot5.id} ${UUID.randomUUID()}"
        val command2 = "fight ${robot2.player} ${robot2.id} ${robot6.id} ${UUID.randomUUID()}"
        val command3 = "fight ${robot3.player} ${robot3.id} ${robot7.id} ${UUID.randomUUID()}"
        val command4 = "fight ${robot4.player} ${robot4.id} ${robot8.id} ${UUID.randomUUID()}"
        val command5 = "fight ${robot5.player} ${robot5.id} ${robot1.id} ${UUID.randomUUID()}"
        val command6 = "fight ${robot6.player} ${robot6.id} ${robot2.id} ${UUID.randomUUID()}"
        val command7 = "fight ${robot7.player} ${robot7.id} ${robot3.id} ${UUID.randomUUID()}"
        val command8 = "fight ${robot8.player} ${robot8.id} ${robot4.id} ${UUID.randomUUID()}"

        val commands = listOf(command1, command2, command3, command4, command5, command6, command7, command8)

        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(commands))
        }.andExpect {
            status { isOk() }
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
    }

    @Test
    fun `destroyed robots get deleted after combat attacks are executed`() {
        // given
        for (i in 1..3) robot1.upgrade(UpgradeType.DAMAGE)
        assertEquals(10, robot1.attackDamage)
        robotRepository.save(robot1)
        val command = "fight ${robot1.player} ${robot1.id} ${robot5.id} ${UUID.randomUUID()}"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isOk() }
            content { string("Command batch accepted") }
        }.andDo { print() }
        // then
        assertNull(robotRepository.findByIdOrNull(robot5.id))
    }

    @Test
    fun `movement works correctly`() {
        // given
        val targetPlanetDto = GameMapPlanetDto(planet2Id, 3)

        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(targetPlanetDto))
        )

        val command = "move ${robot1.player} ${robot1.id} $planet2Id ${UUID.randomUUID()}"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isOk() }
            content { string("Command batch accepted") }
        }.andDo { print() }
        // then
        val robot = robotRepository.findByIdOrNull(robot1.id)!!
        assertEquals(planet2Id, robot.planet.planetId)
        assertEquals(17, robot.energy)
    }

    @Test
    fun `block works correctly`() {
        // given
        val command = "block ${robot1.player} ${robot1.id} ${UUID.randomUUID()}"

        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isOk() }
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
        // given
        val targetPlanetDto = GameMapPlanetDto(planet2Id, 3)

        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(targetPlanetDto))
        )

        val command1 = "block ${robot1.player} ${robot2.id} ${UUID.randomUUID()}"
        val command2 = "move ${robot2.player} ${robot2.id} $planet2Id ${UUID.randomUUID()}"
        val commands = listOf(command1, command2)

        // when
        commands.forEach {
            mockMvc.post("/commands") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(CommandDTO(listOf(it)))
            }.andExpect {
                status { isOk() }
                content { string("Command batch accepted") }
            }.andDo { print() }
        }
        // then
        assertEquals(planet1Id, robot2.planet.planetId)
    }

    @Test
    fun `robot correctly regenerates energy`() {
        // given
        robot1.move(Planet(planet2Id), 10)
        assertEquals(10, robot1.energy)
        robotRepository.save(robot1)
        val command = "regenerate ${robot1.player} ${robot1.id} ${UUID.randomUUID()}"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isOk() }
            content { string("Command batch accepted") }
        }.andDo { print() }
        // then
        assertEquals(14, robotRepository.findByIdOrNull(robot1.id)!!.energy)
    }
}

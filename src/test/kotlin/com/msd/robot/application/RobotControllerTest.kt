package com.msd.robot.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.msd.command.application.CommandController
import com.msd.command.application.CommandDTO
import com.msd.planet.domain.Planet
import com.msd.robot.application.dtos.RobotDto
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
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
class RobotControllerTest(
    @Autowired private var mockMvc: MockMvc,
    @Autowired private var commandController: CommandController,
    @Autowired private var robotRepository: RobotRepository,
    @Autowired private var mapper: ObjectMapper,
) {

    val player1 = UUID.fromString("d43608d5-2107-47a0-bd4f-6720dfa53c4d")

    val planet1 = UUID.fromString("8f3c39b1-c439-4646-b646-ace4839d8849")

    val robot1 = robotRepository.save(Robot(player1, Planet(UUID.randomUUID())))

    @Test
    fun `Sending Spawn Command with invalid planet UUID returns 400`() {
        val spawnDtoInvalidPlanetUUID = """
            {
                "transaction_id": "${UUID.randomUUID()}",
                "player": "$player1",
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
                "planet": "$planet1"
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
                "player": "$player1",
                "planet": "$planet1"
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
        val spawnDto = """
            {
                "transaction_id": "${UUID.randomUUID()}",
                "player": "$player1",
                "planet": "$planet1"
            }
        """.trimIndent()

        val result = mockMvc.post("/robots") {
            contentType = MediaType.APPLICATION_JSON
            content = spawnDto
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val resultRobot = mapper.readValue(result.response.contentAsString, RobotDto::class.java)

        assert(robotRepository.existsById(resultRobot.id))
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
        Assertions.assertEquals(10, robot1.energy)

        val command = "regenerate ${UUID.randomUUID()} invalidTransactionId"
        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(listOf(command)))
        }.andExpect {
            status { isBadRequest() }
        }

        Assertions.assertEquals(10, robotRepository.findByIdOrNull(robot1.id)!!.energy)
    }

    @Test
    fun `Sending an EnergyRegen command leads to a robot's energy being increased`() {

        // given
        robot1.move(Planet(UUID.randomUUID()), 10)
        Assertions.assertEquals(10, robot1.energy)
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
        Assertions.assertEquals(14, robotRepository.findByIdOrNull(robot1.id)!!.energy)
    }

    @Test
    fun `Sending an EnergyRegen command doesnt lead to a robot's energy being increased past max Energy amount`() {
        // given
        robot1.move(Planet(UUID.randomUUID()), 1)
        Assertions.assertEquals(19, robot1.energy)
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
        Assertions.assertEquals(20, robotRepository.findByIdOrNull(robot1.id)!!.energy)
    }
}

package com.msd.robot.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.planet.domain.Planet
import com.msd.robot.application.dtos.RestorationDTO
import com.msd.robot.application.dtos.RobotDto
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
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
    @Autowired private var robotRepository: RobotRepository,
    @Autowired private var mapper: ObjectMapper,
) {

    val player1Id: UUID = UUID.fromString("d43608d5-2107-47a0-bd4f-6720dfa53c4d")

    val planet1Id: UUID = UUID.fromString("8f3c39b1-c439-4646-b646-ace4839d8849")

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
        val spawnDto = """
            {
                "transaction_id": "${UUID.randomUUID()}",
                "player": "$player1Id",
                "planet": "$planet1Id"
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
    fun `passing a wrong UUID returns a 404 when restoring`() {
        // given
        val restorationDTO = """
            {
                "transaction-id": "${UUID.randomUUID()}",
                "restoration-type": "HEALTH"
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
                "transaction-id": "${UUID.randomUUID()}",
                "restoration-type": "WRONG"
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
}

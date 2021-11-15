package com.msd.command.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["no-async"])
class CommandControllerTest(
    @Autowired private var mockMvc: MockMvc,
    @Autowired private var commandController: CommandController,
    @Autowired private var robotRepository: RobotRepository,
    @Autowired private var mapper: ObjectMapper
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
    }

    @Test
    fun `application context loads`() {
        assertNotNull(commandController)
        assertNotNull(mockMvc)
        assertNotNull(robotRepository)
        assertNotNull(mapper)
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

        val commandList = listOf(command1, command2, command3, command4, command5, command6, command7, command8)

        // when
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(CommandDTO(commandList))
        }.andExpect {
            status { isOk() }
            content { string("Command batch accepted") }
        }.andDo { print() }

        // then
        assertAll(
            "Check all robot values",
            {
                assertEquals(9, robotRepository.findByIdOrNull(robot1.id)!!.health)
                assertEquals(19, robotRepository.findByIdOrNull(robot1.id)!!.energy)
            },
            {
                assertEquals(9, robotRepository.findByIdOrNull(robot2.id)!!.health)
                assertEquals(19, robotRepository.findByIdOrNull(robot2.id)!!.energy)
            },
            {
                assertEquals(9, robotRepository.findByIdOrNull(robot3.id)!!.health)
                assertEquals(19, robotRepository.findByIdOrNull(robot3.id)!!.energy)
            },
            {
                assertEquals(9, robotRepository.findByIdOrNull(robot4.id)!!.health)
                assertEquals(19, robotRepository.findByIdOrNull(robot4.id)!!.energy)
            },
            {
                assertEquals(9, robotRepository.findByIdOrNull(robot5.id)!!.health)
                assertEquals(19, robotRepository.findByIdOrNull(robot5.id)!!.energy)
            },
            {
                assertEquals(9, robotRepository.findByIdOrNull(robot6.id)!!.health)
                assertEquals(19, robotRepository.findByIdOrNull(robot6.id)!!.energy)
            },
            {
                assertEquals(9, robotRepository.findByIdOrNull(robot7.id)!!.health)
                assertEquals(19, robotRepository.findByIdOrNull(robot7.id)!!.energy)
            },
            {
                assertEquals(9, robotRepository.findByIdOrNull(robot8.id)!!.health)
                assertEquals(19, robotRepository.findByIdOrNull(robot8.id)!!.energy)
            },
        )
    }
}

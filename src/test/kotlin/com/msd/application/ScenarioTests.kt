package com.msd.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.command.application.CommandControllerTest
import com.msd.command.application.CommandDTO
import com.msd.robot.application.dtos.RobotDto
import com.msd.robot.application.dtos.RobotSpawnDto
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["no-async"])
class ScenarioTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val mapper: ObjectMapper
) {

    val planet1 = UUID.randomUUID()
    val planet2 = UUID.randomUUID()
    val planet3 = UUID.randomUUID()

    val player1 = UUID.randomUUID()
    val player2 = UUID.randomUUID()

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

    /**
     * 1. Scenario:
     *
     * User creates 5 robots of two different players, moves them onto the same planet and lets
     * them fight each other until two robots get destroyed.
     */
    @Test
    fun firstScenario() {
        // //////////////////////  1. Spawn the robots  //////////////////////////////
        // player1, on planet1
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

        // player2, on planet2
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

        // //////////////////// 2. Move the robots to the same planet /////////////////////////
        // All robots move to the same planet, planet3
        val targetPlanetDto = GameMapPlanetDto(planet3, 3)
        CommandControllerTest.mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(targetPlanetDto))
        )

        val moveCommands = listOf(
            "move $player1 ${robot1.id} $planet3 ${UUID.randomUUID()}",
            "move $player1 ${robot2.id} $planet3 ${UUID.randomUUID()}",
            "move $player1 ${robot3.id} $planet3 ${UUID.randomUUID()}",
            "move $player2 ${robot4.id} $planet3 ${UUID.randomUUID()}",
            "move $player2 ${robot5.id} $planet3 ${UUID.randomUUID()}"
        )
        val commandDto = CommandDTO(moveCommands)
        mockMvc.post("/commands") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(commandDto)
        }

        // 3. Fight

        // 4. Retrieve and check robot status
    }
}

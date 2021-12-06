package com.msd.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.msd.application.dto.GameMapNeighbourDto
import com.msd.application.dto.GameMapPlanetDto
import com.msd.command.application.CommandDTO
import com.msd.event.application.ProducerTopicConfiguration
import com.msd.planet.application.PlanetMapper
import com.msd.planet.domain.MapDirection
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
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["no-async"])
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://\${spring.kafka.bootstrap-servers}", "port=9092"]
)
class ScenarioTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val mapper: ObjectMapper,
    @Autowired private val embeddedKafka: EmbeddedKafkaBroker,
    @Autowired private val topicConfig: ProducerTopicConfiguration,
    @Autowired private val planetMapper: PlanetMapper
) : AbstractKafkaProducerTest(embeddedKafka, topicConfig) {

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
            println("started server on port 8080")
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
        startMovementContainer()
        startNeighboursContainer()
        startFightingContainer()
        startResourceDistributionContainer()

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
            planet3, 3, null,
            listOf(
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

        consumerRecords.forEach {
            println(it.topic())
        }
    }
}

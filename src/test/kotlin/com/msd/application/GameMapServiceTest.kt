package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.dto.GameMapNeighbourDto
import com.msd.application.dto.GameMapPlanetDto
import com.msd.config.kafka.core.FailureException
import com.msd.planet.domain.MapDirection
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.util.UUID.*

@SpringBootTest
@ActiveProfiles(profiles = ["test"])
class GameMapServiceTest(
    @Autowired val gameMapService: GameMapService
) {

    val mockGameServiceWebClient = MockWebServer()

    @BeforeEach
    fun setUp() {
        mockGameServiceWebClient.start(port = 8081)
    }

    @AfterEach
    fun tearDown() {
        mockGameServiceWebClient.shutdown()
    }

    @Test
    fun `Returns correct GameMapPlanetDto`() {
        // given
        val startPlanetDto = GameMapNeighbourDto(randomUUID(), 3, MapDirection.NORTH)
        val targetPlanetDto = GameMapPlanetDto(
            randomUUID(),
            3,
            neighbours = listOf(startPlanetDto)
        )

        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(targetPlanetDto))
        )

        // when
        val responsePlanetDto = gameMapService.retrieveTargetPlanetIfRobotCanReach(startPlanetDto.planetId, targetPlanetDto.id)

        // then
        assertEquals(targetPlanetDto.id, responsePlanetDto.id)
    }

    @Test
    fun `RetrieveTargetPlanetIfRobotCanReach throws FailureException if the GameMap Service returns a 400`() {
        // given
        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(400)
        )

        // then
        assertThrows<FailureException> {
            gameMapService.retrieveTargetPlanetIfRobotCanReach(randomUUID(), randomUUID())
        }
    }

    @Test
    fun `RetrieveTargetPlanetIfRobotCanReach throws ClientException if the GameMap Service returns a 500`() {
        // given
        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        // then
        val exception = assertThrows<ClientException> {
            gameMapService.retrieveTargetPlanetIfRobotCanReach(randomUUID(), randomUUID())
        }
        assertEquals(
            "Could not get planet data from MapService",
            exception.message
        )
    }

    @Test
    fun `Throws ClientException if the GameMap service is not reachable`() {
        // given
        mockGameServiceWebClient.shutdown()

        // when then
        val exception = assertThrows<ClientException> {
            gameMapService.retrieveTargetPlanetIfRobotCanReach(randomUUID(), randomUUID())
        }
        assertEquals("Could not connect to Map Service", exception.message)
    }

    @Test
    fun `returns a list of planets`() {
        // given
        val planetDto1 = GameMapPlanetDto(randomUUID(), 3)
        val planetDto2 = GameMapPlanetDto(randomUUID(), 3)
        val planetDto3 = GameMapPlanetDto(randomUUID(), 3)
        val planetDto4 = GameMapPlanetDto(randomUUID(), 3)

        val planetDTOs = listOf(planetDto1, planetDto2, planetDto3, planetDto4)
        mockGameServiceWebClient.enqueue(
            MockResponse().setResponseCode(200).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jacksonObjectMapper().writeValueAsString(planetDTOs))
        )
        // when
        val responsePlanets = gameMapService.getAllPlanets()
        // then
        assertEquals(planetDTOs.size, responsePlanets.size)
        assert(responsePlanets.containsAll(responsePlanets))
    }
}

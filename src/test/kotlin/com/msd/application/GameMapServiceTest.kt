package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.robot.application.exception.TargetPlanetNotReachableException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.UUID.*

class GameMapServiceTest {

    companion object {
        val mockGameServiceWebClient = MockWebServer()
        val gameMapService = GameMapService()

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

    @Test
    fun `Returns correct GameMapPlanetDto`() {
        // given
        val targetPlanetDto = GameMapPlanetDto(randomUUID(), 3)

        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jacksonObjectMapper().writeValueAsString(targetPlanetDto))
        )

        // when
        val responsePlanetDto = gameMapService.retrieveTargetPlanetIfRobotCanReach(randomUUID(), targetPlanetDto.id)

        // then
        assertEquals(targetPlanetDto.id, responsePlanetDto.id)
    }

    @Test
    fun `Throws InvalidMoveException if the GameMap Service returns a 400`() {
        // given
        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(400)
        )

        // then
        assertThrows<TargetPlanetNotReachableException> {
            gameMapService.retrieveTargetPlanetIfRobotCanReach(randomUUID(), randomUUID())
        }
    }

    @Test
    fun `Throws ClientException if the GameMap Service returns a 500`() {
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
            "GameMap Client returned internal error when retrieving targetPlanet for movement",
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
        assertEquals("Could not connect to GameMap client", exception.message)
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

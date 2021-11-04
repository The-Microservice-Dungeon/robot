package com.msd.robot.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.domain.PlanetType
import junit.framework.Assert.assertEquals
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        val targetPlanetDto = GameMapPlanetDto(randomUUID(), 3, PlanetType.STANDARD, randomUUID())

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
        assertThrows<InvalidMoveException> {
            gameMapService.retrieveTargetPlanetIfRobotCanReach(randomUUID(), randomUUID())
        }
    }

    @Test
    fun `Throws ClientInternalException if the GameMap Service returns a 500`() {
        // given
        mockGameServiceWebClient.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        // then
        assertThrows<ClientInternalException> {
            gameMapService.retrieveTargetPlanetIfRobotCanReach(randomUUID(), randomUUID())
        }
    }
}
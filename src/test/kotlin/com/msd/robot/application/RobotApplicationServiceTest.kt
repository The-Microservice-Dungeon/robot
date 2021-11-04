package com.msd.robot.application

import com.msd.domain.Planet
import com.msd.domain.PlanetType
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import junit.framework.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*
import javax.transaction.Transactional

@SpringBootTest
@RunWith(MockitoJUnitRunner::class)
class RobotApplicationServiceTest {

    lateinit var robot1: Robot
    lateinit var robot2: Robot
    lateinit var unknownRobotId: UUID

    lateinit var planet1: Planet
    lateinit var planet2: Planet

    @Autowired
    lateinit var robotRepository: RobotRepository
    @Mock
    lateinit var gameMapMockService: GameMapService
    lateinit var robotApplicationService: RobotApplicationService

    val player1: UUID = UUID.randomUUID()
    val player2: UUID = UUID.randomUUID()

    @BeforeEach
    @Transactional
    fun setup() {
        robotApplicationService = RobotApplicationService(robotRepository, gameMapMockService)

        planet1 = Planet(UUID.randomUUID(), PlanetType.SPACE_STATION, null)
        planet2 = Planet(UUID.randomUUID(), PlanetType.STANDARD, null)

        robot1 = robotRepository.save(Robot(UUID.randomUUID(), planet1))
        robot2 = robotRepository.save(Robot(UUID.randomUUID(), planet2))
        unknownRobotId = UUID.randomUUID()
    }

    @Test
    fun `Movement command specifies unknown robotId`() {
        // given
        val command = MovementCommand(unknownRobotId, planet1.planetId, player1)

        // when
        robotApplicationService.move(command)

        // then
        // TODO check if event got thrown
    }

    @Test
    fun `Movement command specifies different player than robot owner`() {
        // given
        val command = MovementCommand(robot1.id, planet2.planetId, robot2.player)

        // when
        robotApplicationService.move(command)

        // then
        assertEquals(planet1, robot1.planet)
    }

    @Test
    fun `If GameMap Service returns impossible path, robot doesn't move`() {
        // given
        val command = MovementCommand(robot1.id, planet2.planetId, robot1.player)
        Mockito
            .`when`(gameMapMockService.retrieveTargetPlanetIfRobotCanReach(UUID.randomUUID(), UUID.randomUUID()))
            .thenThrow(InvalidMoveException(""))

        // when
        robotApplicationService.move(command)

        // then
        assertEquals(planet1, robot1.planet)
    }

    @Test
    fun `Robot moves if there are no problems`() {
        // given
        val command = MovementCommand(robot1.id, planet2.planetId, robot1.player)
        val planetDto = GameMapPlanetDto(planet2.planetId, 3, planet2.type, planet2.playerId)
        Mockito
            .`when`(gameMapMockService.retrieveTargetPlanetIfRobotCanReach(UUID.randomUUID(), UUID.randomUUID()))
            .thenReturn(planetDto)

        // when
        robotApplicationService.move(command)

        // then
        assertEquals(planet1, robot1.planet)
    }
}

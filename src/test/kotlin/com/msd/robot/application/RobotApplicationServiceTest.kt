package com.msd.robot.application

import com.msd.application.ClientException
import com.msd.application.GameMapPlanetDto
import com.msd.application.GameMapService
import com.msd.command.MovementCommand
import com.msd.command.RegenCommand
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetType
import com.msd.robot.domain.NotEnoughEnergyException
import com.msd.robot.domain.PlanetBlockedException
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import java.util.*

@ExtendWith(MockKExtension::class)
class RobotApplicationServiceTest {

    lateinit var robot1: Robot
    lateinit var robot2: Robot
    lateinit var robot3: Robot
    lateinit var unknownRobotId: UUID

    lateinit var planet1: Planet
    lateinit var planet2: Planet

    @MockK
    lateinit var robotRepository: RobotRepository

    @MockK
    lateinit var gameMapMockService: GameMapService
    lateinit var robotApplicationService: RobotApplicationService

    val player1: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        robotApplicationService = RobotApplicationService(robotRepository, gameMapMockService)

        planet1 = Planet(UUID.randomUUID(), PlanetType.SPACE_STATION, null)
        planet2 = Planet(UUID.randomUUID(), PlanetType.STANDARD, null)

        robot1 = Robot(UUID.randomUUID(), planet1)
        robot2 = Robot(UUID.randomUUID(), planet2)
        robot3 = Robot(UUID.randomUUID(), planet1)
        unknownRobotId = UUID.randomUUID()
    }

    @Test
    fun `Robot doesn't move if it is unknown`() {
        // given
        val command = MovementCommand(unknownRobotId, player1, planet1.planetId)
        every { robotRepository.findByIdOrNull(unknownRobotId) } returns null
        // then
        assertThrows<RobotNotFoundException> { robotApplicationService.move(command) }
    }

    @Test
    fun `Robot doesn't move if players don't`() {
        // given
        val command = MovementCommand(robot1.id, robot2.player, planet2.planetId)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        // when
        assertThrows<InvalidPlayerException> { robotApplicationService.move(command) }
        // then
        assertEquals(planet1, robot1.planet)
    }

    @Test
    fun `If GameMap Service returns impossible path, robot doesn't move`() {
        // given
        val command = MovementCommand(robot1.id, robot1.player, planet2.planetId)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every {
            gameMapMockService.retrieveTargetPlanetIfRobotCanReach(
                any(),
                any()
            )
        } throws TargetPlanetNotReachableException("")

        // when
        assertThrows<TargetPlanetNotReachableException> {
            robotApplicationService.move(command)
        }

        // then
        assertEquals(planet1, robot1.planet)
    }

    @Test
    fun `Robot doesn't move when GameMap MicroService is not reachable`() {
        // given
        val command = MovementCommand(robot1.id, robot1.player, planet2.planetId)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { gameMapMockService.retrieveTargetPlanetIfRobotCanReach(any(), any()) } throws ClientException("")

        // when
        assertThrows<ClientException> {
            robotApplicationService.move(command)
        }

        // then
        assertEquals(planet1, robot1.planet)
    }

    @Test
    fun `Robot can't move if it has not enough energy`() {
        // given
        while (robot1.energy >= 4) // blocking on Level 0 costs 4 energy
            robot1.block()
        planet1.blocked = false
        val command = MovementCommand(robot1.id, robot1.player, planet2.planetId)
        val planetDto = GameMapPlanetDto(planet2.planetId, 3, planet2.type, planet2.playerId)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { gameMapMockService.retrieveTargetPlanetIfRobotCanReach(any(), any()) } returns planetDto

        // when
        assertThrows<NotEnoughEnergyException> {
            robotApplicationService.move(command)
        }

        // then
        assertEquals(planet1, robot1.planet)
    }

    @Test
    fun `Robot can't move out of a blocked planet`() {
        // given
        robot1.block()

        val command = MovementCommand(robot3.id, robot3.player, planet2.planetId)
        val planetDto = GameMapPlanetDto(planet2.planetId, 3, planet2.type, planet2.playerId)
        every { robotRepository.findByIdOrNull(robot3.id) } returns robot3
        every { gameMapMockService.retrieveTargetPlanetIfRobotCanReach(any(), any()) } returns planetDto

        // when
        assertThrows<PlanetBlockedException> {
            robotApplicationService.move(command)
        }

        // then
        assertEquals(planet1, robot3.planet)
    }

    @Test
    fun `Robot moves if there are no problems`() {
        // given
        val command = MovementCommand(robot1.id, robot1.player, planet2.planetId)
        val planetDto = GameMapPlanetDto(planet2.planetId, 3, planet2.type, planet2.playerId)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.save(any()) } returns robot1
        every { gameMapMockService.retrieveTargetPlanetIfRobotCanReach(any(), any()) } returns planetDto

        // when
        robotApplicationService.move(command)

        // then
        assertEquals(planet2, robot1.planet)
        verify(exactly = 1) { robotRepository.save(robot1) }
    }

    @Test
    fun `Unknown robotId when regenerating causes an exception to be thrown`() {
        // given
        every { robotRepository.findByIdOrNull(unknownRobotId) } returns null
        // then
        assertThrows<RobotNotFoundException> {
            robotApplicationService.regenerateEnergy(RegenCommand(unknownRobotId, UUID.randomUUID()))
        }
    }

    @Test
    fun `playerId not matching ownerId when regenerating causes an exception to be thrown`() {
        // given
        robot1.move(Planet(UUID.randomUUID()), 10)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1

        // then
        assertThrows<InvalidPlayerException> {
            robotApplicationService.regenerateEnergy(RegenCommand(robot1.id, UUID.randomUUID()))
        }
    }

    @Test
    fun `Robot energy increases when regenerating`() {
        // given
        robot1.move(Planet(UUID.randomUUID()), 6)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.save(robot1) } returns robot1
        // when
        robotApplicationService.regenerateEnergy(RegenCommand(robot1.id, robot1.player))

        // then
        assertEquals(18, robot1.energy)
        verify(exactly = 1) { robotRepository.save(robot1) }
    }
}

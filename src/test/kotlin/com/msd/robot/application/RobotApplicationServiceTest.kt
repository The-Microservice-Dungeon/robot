package com.msd.robot.application

import com.msd.application.ClientException
import com.msd.application.GameMapPlanetDto
import com.msd.application.GameMapService
import com.msd.command.BlockCommand
import com.msd.command.MovementCommand
import com.msd.command.RegenCommand
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetType
import com.msd.robot.domain.*
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertAll
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
    lateinit var robotDomainService: RobotDomainService

    val player1: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        robotDomainService = RobotDomainService(robotRepository)
        robotApplicationService = RobotApplicationService(gameMapMockService, robotDomainService)

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
    fun `Robot doesn't move if players don't match`() {
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
        assertEquals(10, robot1.energy)
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

    @Test
    fun `Robot blocks planet successfully`() {
        // given
        val command = BlockCommand(robot1.id, robot1.player)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.save(any()) } returns robot1

        // when
        robotApplicationService.block(command)

        // then
        assert(robot1.planet.blocked)
        assert(robot3.planet.blocked)
    }

    @Test
    fun `Robot cannot block planet if it has not enough energy`() {
        // given
        robot1.move(planet1, 19)
        val command = BlockCommand(robot1.id, robot1.player)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1

        // when
        assertThrows<NotEnoughEnergyException> {
            robotApplicationService.block(command)
        }

        // then
        assert(!robot1.planet.blocked)
    }

    @Test
    fun `Robot can't block planet if players don't match`() {
        // given
        val command = BlockCommand(robot1.id, UUID.randomUUID())
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        // when
        assertThrows<InvalidPlayerException> {
            robotApplicationService.block(command)
        }
        // then
        assert(!robot1.planet.blocked)
    }

    @Test
    fun `Throws exception when upgrading Robot if robotId is unknown`() {
        // given
        every { robotRepository.findByIdOrNull(unknownRobotId) } returns null
        // then
        assertThrows<RobotNotFoundException> {
            robotApplicationService.upgrade(unknownRobotId, UpgradeType.HEALTH, 1)
        }
    }

    @Test
    fun `Can't skip upgrade levels`() {
        // given
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        // when
        assertThrows<UpgradeException>("Cannot skip upgrade levels. Tried to upgrade from level 0 to level 2") {
            robotApplicationService.upgrade(robot1.id, UpgradeType.HEALTH, 2)
        }
        // then
        assertEquals(0, robot1.healthLevel)
    }

    @Test
    fun `Can't downgrade`() {
        // given
        robot1.upgrade(UpgradeType.HEALTH, 1)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        // when
        assertThrows<UpgradeException>("Cannot downgrade Robot. Tried to go from level 1 to level 0") {
            robotApplicationService.upgrade(robot1.id, UpgradeType.HEALTH, 0)
        }
        // then
        assertEquals(1, robot1.healthLevel)
    }

    @Test
    fun `Can't upgrade past max level`() {
        // given
        for (i in 1..5) robot1.upgrade(UpgradeType.HEALTH, i)
        for (i in 1..4) robot1.upgrade(UpgradeType.MINING, i) // Mining level max is level 4
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        // when
        assertAll(
            {
                assertThrows<UpgradeException>("Max Health Level has been reached. Upgrade not possible.") {
                    robotApplicationService.upgrade(robot1.id, UpgradeType.HEALTH, 6)
                }
            },
            {
                assertThrows<UpgradeException>("Max Mining has been reached. Upgrade not possible.") {
                    robotApplicationService.upgrade(robot1.id, UpgradeType.MINING, 5)
                }
            }
        )

        // then
        assertEquals(5, robot1.healthLevel)
        assertEquals(4, robot1.miningLevel)
    }

    @Test
    fun `Upgrading changes a robots level`() {
        // given
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        // when
        robotApplicationService.upgrade(robot1.id, UpgradeType.HEALTH, 1)
        robotApplicationService.upgrade(robot1.id, UpgradeType.MINING, 1)
        robotApplicationService.upgrade(robot1.id, UpgradeType.STORAGE, 1)

        // then
        assertAll(
            {
                assertEquals(1, robot1.healthLevel)
            },
            {
                assertEquals(1, robot1.miningLevel)
            },
            {
                assertEquals(1, robot1.inventory.storageLevel)
            }
        )
    }
}

package com.msd.robot.application

import com.msd.application.ClientException
import com.msd.application.ExceptionConverter
import com.msd.application.GameMapPlanetDto
import com.msd.application.GameMapService
import com.msd.command.application.AttackCommand
import com.msd.command.application.BlockCommand
import com.msd.command.application.EnergyRegenCommand
import com.msd.command.application.MovementCommand
import com.msd.domain.ResourceType
import com.msd.planet.domain.Planet
import com.msd.robot.domain.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import java.util.*

@ExtendWith(MockKExtension::class)
class RobotApplicationServiceTest {

    lateinit var robot1: Robot
    lateinit var robot2: Robot
    lateinit var robot3: Robot
    lateinit var robot4: Robot
    lateinit var robot5: Robot
    lateinit var robot6: Robot
    lateinit var unknownRobotId: UUID

    private val player1Id: UUID = UUID.randomUUID()
    private val player2Id: UUID = UUID.randomUUID()

    lateinit var planet1: Planet
    lateinit var planet2: Planet

    @MockK
    lateinit var robotRepository: RobotRepository

    @MockK
    lateinit var gameMapMockService: GameMapService

    @MockK
    lateinit var exceptionConverter: ExceptionConverter

    lateinit var robotApplicationService: RobotApplicationService
    lateinit var robotDomainService: RobotDomainService

    lateinit var robotIdsToRobot: Map<UUID, Robot>

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        robotDomainService = RobotDomainService(robotRepository)
        robotApplicationService = RobotApplicationService(gameMapMockService, robotDomainService, exceptionConverter)

        planet1 = Planet(UUID.randomUUID())
        planet2 = Planet(UUID.randomUUID())

        robot1 = Robot(player1Id, planet1)
        robot2 = Robot(player1Id, planet2)
        robot3 = Robot(player1Id, planet1)
        robot4 = Robot(player2Id, planet1)
        robot5 = Robot(player2Id, planet2)
        robot6 = Robot(player2Id, planet1)
        unknownRobotId = UUID.randomUUID()

        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.findByIdOrNull(robot2.id) } returns robot2
        every { robotRepository.findByIdOrNull(robot3.id) } returns robot3
        every { robotRepository.findByIdOrNull(robot4.id) } returns robot4
        every { robotRepository.findByIdOrNull(robot5.id) } returns robot5
        every { robotRepository.findByIdOrNull(robot6.id) } returns robot6

        every { robotRepository.save(any()) } returns robot1 // we don't use the return value of save calls
    }

    @Test
    fun `Robot doesn't move if it is unknown`() {
        // given
        val command = MovementCommand(player1Id, unknownRobotId, planet1.planetId, UUID.randomUUID())
        every { robotRepository.findByIdOrNull(unknownRobotId) } returns null
        // then
        assertThrows<RobotNotFoundException> { robotApplicationService.move(command) }
    }

    @Test
    fun `Robot doesn't move if players don't match`() {
        // given
        val command = MovementCommand(robot4.player, robot1.id, planet2.planetId, UUID.randomUUID())
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        // when
        assertThrows<InvalidPlayerException> { robotApplicationService.move(command) }
        // then
        assertEquals(planet1, robot1.planet)
    }

    @Test
    fun `If GameMap Service returns impossible path, robot doesn't move`() {
        // given
        val command = MovementCommand(robot1.player, robot1.id, planet2.planetId, UUID.randomUUID())
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
        val command = MovementCommand(robot1.player, robot1.id, planet2.planetId, UUID.randomUUID())
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
        val command = MovementCommand(robot1.player, robot1.id, planet2.planetId, UUID.randomUUID())
        val planetDto = GameMapPlanetDto(planet2.planetId, 3)
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

        val command = MovementCommand(robot3.player, robot3.id, planet2.planetId, UUID.randomUUID())
        val planetDto = GameMapPlanetDto(planet2.planetId, 3)
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
        val command = MovementCommand(robot1.player, robot1.id, planet2.planetId, UUID.randomUUID())
        val planetDto = GameMapPlanetDto(planet2.planetId, 3)
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
            robotApplicationService.regenerateEnergy(EnergyRegenCommand(UUID.randomUUID(), unknownRobotId, UUID.randomUUID()))
        }
    }

    @Test
    fun `playerId not matching ownerId when regenerating causes an exception to be thrown`() {
        // given
        robot1.move(Planet(UUID.randomUUID()), 10)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1

        // then
        assertThrows<InvalidPlayerException> {
            robotApplicationService.regenerateEnergy(EnergyRegenCommand(UUID.randomUUID(), robot1.id, UUID.randomUUID()))
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
        robotApplicationService.regenerateEnergy(EnergyRegenCommand(robot1.player, robot1.id, UUID.randomUUID()))

        // then
        assertEquals(18, robot1.energy)
        verify(exactly = 1) { robotRepository.save(robot1) }
    }

    @Test
    fun `Robot blocks planet successfully`() {
        // given
        val command = BlockCommand(robot1.player, robot1.id, UUID.randomUUID())
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
        val command = BlockCommand(robot1.player, robot1.id, UUID.randomUUID())
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
        val command = BlockCommand(UUID.randomUUID(), robot1.id, UUID.randomUUID())
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        // when
        assertThrows<InvalidPlayerException> {
            robotApplicationService.block(command)
        }
        // then
        assert(!robot1.planet.blocked)
    }

    @Test
    fun `Throws exception when robot is not found`() {
        // given
        every { robotRepository.findByIdOrNull(robot1.id) } returns null

        // then
        assertThrows<RobotNotFoundException> {
            robotApplicationService.repair(robot1.id)
        }
    }

    @Test
    fun `Health regenerates successfully`() {
        // given
        robot1.receiveDamage(5)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.save(robot1) } returns robot1
        // when
        robotApplicationService.repair(robot1.id)

        // then
        assertEquals(robot1.maxHealth, robot1.health)
    }

    @Test
    fun `All AttackCommands from a batch get executed`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot4, robot6)
        every { robotRepository.findAllByPlanet_PlanetId(robot2.planet.planetId) } returns
            listOf(robot2, robot5)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf()
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot2.planet.planetId) } returns listOf()
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf(
            robot1, robot2, robot3, robot4, robot5, robot6
        )

        val attackCommands = listOf(
            AttackCommand(player1Id, robot1.id, robot4.id, UUID.randomUUID()),
            AttackCommand(player1Id, robot2.id, robot5.id, UUID.randomUUID()),
            AttackCommand(player1Id, robot3.id, robot6.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot4.id, robot1.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot5.id, robot2.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot6.id, robot3.id, UUID.randomUUID()),
        )
        // when
        robotApplicationService.executeAttacks(attackCommands)
        // then
        assertAll(
            {
                assertEquals(9, robot1.health)
                assertEquals(19, robot1.energy)
            },
            {
                assertEquals(9, robot2.health)
                assertEquals(19, robot2.energy)
            },
            {
                assertEquals(9, robot3.health)
                assertEquals(19, robot3.energy)
            },
            {
                assertEquals(9, robot4.health)
                assertEquals(19, robot4.energy)
            },
            {
                assertEquals(9, robot5.health)
                assertEquals(19, robot5.energy)
            },
            {
                assertEquals(9, robot6.health)
                assertEquals(19, robot6.energy)
            },
        )
    }

    @Test
    fun `Only an attack on a robot on another planet doesn't get executed`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot4, robot6)
        every { robotRepository.findAllByPlanet_PlanetId(robot2.planet.planetId) } returns
            listOf(robot2, robot5)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf()
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot2.planet.planetId) } returns listOf()
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf(
            robot1, robot2, robot3, robot4, robot5, robot6
        )

        val attackCommands = listOf(
            AttackCommand(player1Id, robot1.id, robot4.id, UUID.randomUUID()),
            AttackCommand(player1Id, robot2.id, robot4.id, UUID.randomUUID()),
            AttackCommand(player1Id, robot3.id, robot6.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot4.id, robot1.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot5.id, robot1.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot6.id, robot3.id, UUID.randomUUID()),
        )
        justRun { exceptionConverter.handle(any(), any()) }
        // when
        robotApplicationService.executeAttacks(attackCommands)
        // then
        assertAll(
            {
                assertEquals(9, robot1.health)
                assertEquals(19, robot1.energy)
            },
            {
                assertEquals(10, robot2.health)
                assertEquals(20, robot2.energy)
            },
            {
                assertEquals(9, robot3.health)
                assertEquals(19, robot3.energy)
            },
            {
                assertEquals(9, robot4.health)
                assertEquals(19, robot4.energy)
            },
            {
                assertEquals(10, robot5.health)
                assertEquals(20, robot5.energy)
            },
            {
                assertEquals(9, robot6.health)
                assertEquals(19, robot6.energy)
            },
        )
        verify(exactly = 2) {
            exceptionConverter.handle(any(), any())
        }
    }

    @Test
    fun `Resources of dead robots get distributed correctly`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot4, robot6)
        every { robotRepository.findAllByPlanet_PlanetId(robot2.planet.planetId) } returns
            listOf(robot2, robot5)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf()
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot2.planet.planetId) } returns listOf()
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf(
            robot1, robot2, robot3, robot4, robot5, robot6
        )
        justRun { robotRepository.delete(robot4) }

        ResourceType.values().forEach { robot4.inventory.addResource(it, 4) }

        val attackCommands = listOf(
            AttackCommand(player1Id, robot1.id, robot4.id, UUID.randomUUID()),
            AttackCommand(player1Id, robot2.id, robot5.id, UUID.randomUUID()),
            AttackCommand(player1Id, robot3.id, robot4.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot4.id, robot1.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot5.id, robot2.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot6.id, robot4.id, UUID.randomUUID()),
        )

        // Let every robot attack 3 times
        for (i in 1..3) robotApplicationService.executeAttacks(attackCommands)

        // This time one robot will die, so the repo has to return the correct robot
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf(
            robot4
        )
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot6)

        // when
        // now Robot 4 dies
        robotApplicationService.executeAttacks(attackCommands)

        // then
        assertAll(
            {
                assertEquals(false, robot4.alive)
            },
            {
                assertEquals( // Robot 4 had 20 items in its inventory, the others none
                    20,
                    robot1.inventory.usedStorage +
                        robot3.inventory.usedStorage +
                        robot6.inventory.usedStorage
                )
            },
            {
                assertEquals(
                    0,
                    robot2.inventory.usedStorage + robot5.inventory.usedStorage
                )
            }
        )
    }

    @Test
    fun `Invalid Command in batch leads to ExceptionHandler being called and no damage`() {
        // given
        val attackCommands = listOf(
            AttackCommand(player1Id, robot1.id, robot4.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot2.id, robot5.id, UUID.randomUUID()), // invalid player
            AttackCommand(player1Id, robot3.id, robot4.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot4.id, robot1.id, UUID.randomUUID()),
            AttackCommand(player1Id, robot5.id, robot2.id, UUID.randomUUID()), // invalid player
            AttackCommand(player2Id, robot6.id, robot4.id, UUID.randomUUID()),
        )

        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot4, robot6)
        every { robotRepository.findAllByPlanet_PlanetId(robot2.planet.planetId) } returns
            listOf(robot2, robot5)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf()
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot2.planet.planetId) } returns listOf()
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf(
            robot1, robot2, robot3, robot4, robot5, robot6
        )
        justRun { exceptionConverter.handle(any(), any()) }

        // when
        robotApplicationService.executeAttacks(attackCommands)

        // assert
        verify(exactly = 2) {
            exceptionConverter.handle(any(), any())
        }
    }

    @Test
    fun `Not all resources of killed robot can be distributed`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot4, robot6)
        every { robotRepository.findAllByPlanet_PlanetId(robot2.planet.planetId) } returns
            listOf(robot2, robot5)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf()
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot2.planet.planetId) } returns listOf()
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf(
            robot1, robot2, robot3, robot4, robot5, robot6
        )
        justRun { robotRepository.delete(robot4) }

        // distribute initial inventories
        // there is space for only 10 more resources, but when robot 4 dies want to be distributed
        ResourceType.values().forEach {
            robot4.inventory.addResource(it, 4)
            robot1.inventory.addResource(it, 3)
            robot3.inventory.addResource(it, 3)
            robot6.inventory.addResource(it, 4)
        }

        assertAll(
            {
                assertEquals(
                    50,
                    robot1.inventory.usedStorage +
                        robot3.inventory.usedStorage + robot6.inventory.usedStorage
                )
            },
            {
                assertEquals(20, robot4.inventory.usedStorage)
            }
        )

        val attackCommands = listOf(
            AttackCommand(player1Id, robot1.id, robot4.id, UUID.randomUUID()),
            AttackCommand(player1Id, robot3.id, robot4.id, UUID.randomUUID()),
            AttackCommand(player2Id, robot6.id, robot4.id, UUID.randomUUID()),
        )

        // Let every robot attack 3 times
        for (i in 1..3) robotApplicationService.executeAttacks(attackCommands)

        // This time one robot will die, so the repo has to return the correct robot
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf(
            robot4
        )
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot6)

        // when
        // now Robot 4 dies
        robotApplicationService.executeAttacks(attackCommands)

        // then
        assertAll(
            {
                assertEquals(false, robot4.alive)
            },
            {
                assertEquals(
                    60,
                    robot1.inventory.usedStorage +
                        robot3.inventory.usedStorage +
                        robot6.inventory.usedStorage
                )
            },
            {
                assertEquals( // no other robots should get the resources
                    0, robot2.inventory.usedStorage + robot5.inventory.usedStorage
                )
            }
        )
    }
}

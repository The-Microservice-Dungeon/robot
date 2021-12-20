package com.msd.robot.application

import com.msd.application.*
import com.msd.application.dto.GameMapPlanetDto
import com.msd.command.application.command.*
import com.msd.core.FailureException
import com.msd.domain.ResourceType
import com.msd.event.application.EventSender
import com.msd.item.domain.AttackItemType
import com.msd.planet.domain.Planet
import com.msd.robot.application.exception.TargetPlanetNotReachableException
import com.msd.robot.domain.*
import com.msd.robot.domain.exception.NotEnoughEnergyException
import com.msd.robot.domain.exception.PlanetBlockedException
import com.msd.robot.domain.exception.RobotNotFoundException
import com.msd.robot.domain.exception.UpgradeException
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ExtendWith(MockKExtension::class)
@SpringBootTest
@ActiveProfiles(profiles = ["test"])
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
    lateinit var eventSender: EventSender

    @MockK
    lateinit var successEventSender: SuccessEventSender

    lateinit var robotApplicationService: RobotApplicationService
    lateinit var robotDomainService: RobotDomainService

    val randomUUID: UUID
        get() = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        robotDomainService = RobotDomainService(robotRepository, gameMapMockService)
        robotApplicationService = RobotApplicationService(gameMapMockService, robotDomainService, eventSender, successEventSender)

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
        every { eventSender.sendEvent(any(), any(), any()) } returns randomUUID
        justRun { successEventSender.sendAttackItemEvents(any(), any(), any()) }
        justRun { successEventSender.sendMovementItemEvents(any()) }
        every { successEventSender.sendMovementEvents(any(), any(), any(), any()) } returns UUID.randomUUID()
        justRun { eventSender.sendGenericEvent(any(), any()) }
        justRun { successEventSender.sendMiningEvent(any()) }
        justRun { successEventSender.sendFightingEvent(any(), any(), any()) }
        justRun { successEventSender.sendResourceDistributionEvent(any()) }
        justRun { successEventSender.sendEnergyRegenEvent(any(), any()) }
        justRun { successEventSender.sendBlockEvent(any(), any()) }
    }

    @Test
    fun `Robot doesn't move if it is unknown`() {
        // given
        val command = MovementCommand(unknownRobotId, planet1.planetId, UUID.randomUUID())
        every { robotRepository.findByIdOrNull(unknownRobotId) } returns null
        val failureException = slot<FailureException>()
        justRun { eventSender.handleException(capture(failureException), any()) }

        // when
        robotApplicationService.executeMoveCommands(listOf(command))

        // then
        assert(failureException.captured is RobotNotFoundException)
    }

    @Test
    fun `If GameMap Service returns impossible path, robot doesn't move`() {
        // given
        val command = MovementCommand(robot1.id, planet2.planetId, UUID.randomUUID())
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every {
            gameMapMockService.retrieveTargetPlanetIfRobotCanReach(
                any(),
                any()
            )
        } throws TargetPlanetNotReachableException("")
        val failureException = slot<FailureException>()
        justRun { eventSender.handleException(capture(failureException), any()) }

        // when
        robotApplicationService.executeMoveCommands(listOf(command))

        // then
        assertEquals(planet1, robot1.planet)
        assert(failureException.captured is TargetPlanetNotReachableException)
    }

    @Test
    fun `Robot doesn't move when GameMap MicroService is not reachable`() {
        // given
        val command = MovementCommand(robot1.id, planet2.planetId, UUID.randomUUID())
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { gameMapMockService.retrieveTargetPlanetIfRobotCanReach(any(), any()) } throws ClientException("")

        // when
        assertThrows<ClientException> {
            robotApplicationService.executeMoveCommands(listOf(command))
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
        val command = MovementCommand(robot1.id, planet2.planetId, UUID.randomUUID())
        val planetDto = GameMapPlanetDto(planet2.planetId, 3)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { gameMapMockService.retrieveTargetPlanetIfRobotCanReach(any(), any()) } returns planetDto
        val failureException = slot<FailureException>()
        justRun { eventSender.handleException(capture(failureException), any()) }

        // when
        robotApplicationService.executeMoveCommands(listOf(command))

        // then
        assertEquals(planet1, robot1.planet)
        assert(failureException.captured is NotEnoughEnergyException)
    }

    @Test
    fun `Robot can't move out of a blocked planet`() {
        // given
        robot1.block()

        val command = MovementCommand(robot3.id, planet2.planetId, UUID.randomUUID())
        val planetDto = GameMapPlanetDto(planet2.planetId, 3)
        every { robotRepository.findByIdOrNull(robot3.id) } returns robot3
        every { gameMapMockService.retrieveTargetPlanetIfRobotCanReach(any(), any()) } returns planetDto
        val failureException = slot<FailureException>()
        justRun { eventSender.handleException(capture(failureException), any()) }

        // when
        robotApplicationService.executeMoveCommands(listOf(command))

        // then
        assertEquals(planet1, robot3.planet)
        assert(failureException.captured is PlanetBlockedException)
    }

    @Test
    fun `Robot moves if there are no problems`() {
        // given
        val command = MovementCommand(robot1.id, planet2.planetId, UUID.randomUUID())
        val planetDto = GameMapPlanetDto(planet2.planetId, 3)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.save(any()) } returns robot1
        every { gameMapMockService.retrieveTargetPlanetIfRobotCanReach(any(), any()) } returns planetDto
        every { robotRepository.findAllByPlanet_PlanetId(any()) } returns listOf()

        // when
        robotApplicationService.executeMoveCommands(listOf(command))

        // then
        assertEquals(planet2, robot1.planet)
        verify(atLeast = 1) { robotRepository.save(robot1) }
    }

    @Test
    fun `Unknown robotId when regenerating causes an exception to be thrown`() {
        // given
        every { robotRepository.findByIdOrNull(unknownRobotId) } returns null
        val failureException = slot<FailureException>()
        justRun { eventSender.handleException(capture(failureException), any()) }
        // then
        robotApplicationService.executeEnergyRegenCommands(
            listOf(
                EnergyRegenCommand(
                    unknownRobotId,
                    UUID.randomUUID()
                )
            )
        )

        assert(failureException.captured is RobotNotFoundException)
    }

    @Test
    fun `Robot energy increases when regenerating`() {
        // given
        robot1.move(Planet(UUID.randomUUID()), 6)
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.save(robot1) } returns robot1
        // when
        robotApplicationService.executeEnergyRegenCommands(listOf(EnergyRegenCommand(robot1.id, UUID.randomUUID())))

        // then
        assertEquals(18, robot1.energy)
        verify(exactly = 1) { robotRepository.save(robot1) }
    }

    @Test
    fun `Robot blocks planet successfully`() {
        // given
        val command = BlockCommand(robot1.id, UUID.randomUUID())
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.save(any()) } returns robot1

        // when
        robotApplicationService.executeBlockCommands(listOf(command))

        // then
        assert(robot1.planet.blocked)
        assert(robot3.planet.blocked)
    }

    @Test
    fun `Robot cannot block planet if it has not enough energy`() {
        val failureException = slot<FailureException>()
        justRun { eventSender.handleException(capture(failureException), any()) }
        // given
        robot1.move(planet1, 19)
        val command = BlockCommand(robot1.id, UUID.randomUUID())
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1

        // when
        robotApplicationService.executeBlockCommands(listOf(command))

        // then
        assert(!robot1.planet.blocked)
        verify(exactly = 1) { eventSender.handleException(any(), any()) }
        assert(failureException.captured is NotEnoughEnergyException)
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
    fun `Throws exception when robot is not found`() {
        // given
        every { robotRepository.findByIdOrNull(robot1.id) } returns null

        // then
        assertThrows<RobotNotFoundException> {
            robotApplicationService.repair(robot1.id)
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
        every { robotRepository.save(any()) } returns robot1
        // when
        robotApplicationService.upgrade(robot1.id, UpgradeType.HEALTH, 1)
        robotApplicationService.upgrade(robot1.id, UpgradeType.MINING, 1)
        robotApplicationService.upgrade(robot1.id, UpgradeType.STORAGE, 1)
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

        val fightingCommands = listOf(
            FightingCommand(robot1.id, robot4.id, UUID.randomUUID()),
            FightingCommand(robot2.id, robot5.id, UUID.randomUUID()),
            FightingCommand(robot3.id, robot6.id, UUID.randomUUID()),
            FightingCommand(robot4.id, robot1.id, UUID.randomUUID()),
            FightingCommand(robot5.id, robot2.id, UUID.randomUUID()),
            FightingCommand(robot6.id, robot3.id, UUID.randomUUID()),
        )
        // when
        robotApplicationService.executeAttacks(fightingCommands)
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

        val fightingCommands = listOf(
            FightingCommand(robot1.id, robot4.id, UUID.randomUUID()),
            FightingCommand(robot2.id, robot4.id, UUID.randomUUID()),
            FightingCommand(robot3.id, robot6.id, UUID.randomUUID()),
            FightingCommand(robot4.id, robot1.id, UUID.randomUUID()),
            FightingCommand(robot5.id, robot1.id, UUID.randomUUID()),
            FightingCommand(robot6.id, robot3.id, UUID.randomUUID()),
        )
        justRun { eventSender.handleException(any(), any()) }
        // when
        robotApplicationService.executeAttacks(fightingCommands)
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
            eventSender.handleException(any(), any())
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

        val fightingCommands = listOf(
            FightingCommand(robot1.id, robot4.id, UUID.randomUUID()),
            FightingCommand(robot2.id, robot5.id, UUID.randomUUID()),
            FightingCommand(robot3.id, robot4.id, UUID.randomUUID()),
            FightingCommand(robot4.id, robot1.id, UUID.randomUUID()),
            FightingCommand(robot5.id, robot2.id, UUID.randomUUID()),
            FightingCommand(robot6.id, robot4.id, UUID.randomUUID()),
        )

        // Let every robot attack 3 times
        for (i in 1..3) robotApplicationService.executeAttacks(fightingCommands)

        // This time one robot will die, so the repo has to return the correct robot
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf(
            robot4
        )
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot6)

        // when
        // now Robot 4 dies
        robotApplicationService.executeAttacks(fightingCommands)

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
        val fightingCommands = listOf(
            FightingCommand(robot1.id, robot4.id, UUID.randomUUID()),
            FightingCommand(robot2.id, UUID.randomUUID(), UUID.randomUUID()), // invalid robot id
            FightingCommand(robot3.id, robot4.id, UUID.randomUUID()),
            FightingCommand(robot4.id, robot1.id, UUID.randomUUID()),
            FightingCommand(robot5.id, UUID.randomUUID(), UUID.randomUUID()), // invalid robot id
            FightingCommand(robot6.id, robot4.id, UUID.randomUUID()),
        )

        every { robotRepository.findByIdOrNull(any()) } returns null
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.findByIdOrNull(robot2.id) } returns robot2
        every { robotRepository.findByIdOrNull(robot3.id) } returns robot3
        every { robotRepository.findByIdOrNull(robot4.id) } returns robot4
        every { robotRepository.findByIdOrNull(robot5.id) } returns robot5
        every { robotRepository.findByIdOrNull(robot6.id) } returns robot6
//        every { robotRepository.findByIdOrNull(any())} answers { robots.find { it.id == firstArg() }} TODO why the fuck doesn't this work?
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot4, robot6)
        every { robotRepository.findAllByPlanet_PlanetId(robot2.planet.planetId) } returns
            listOf(robot2, robot5)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf()
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot2.planet.planetId) } returns listOf()
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf(
            robot1, robot2, robot3, robot4, robot5, robot6
        )
        justRun { eventSender.handleException(any(), any()) }

        // when
        robotApplicationService.executeAttacks(fightingCommands)

        // assert
        verify(exactly = 2) {
            eventSender.handleException(any(), any())
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

        val fightingCommands = listOf(
            FightingCommand(robot1.id, robot4.id, UUID.randomUUID()),
            FightingCommand(robot3.id, robot4.id, UUID.randomUUID()),
            FightingCommand(robot6.id, robot4.id, UUID.randomUUID()),
        )

        // Let every robot attack 3 times
        for (i in 1..3) robotApplicationService.executeAttacks(fightingCommands)

        // This time one robot will die, so the repo has to return the correct robot
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(robot1.planet.planetId) } returns listOf(
            robot4
        )
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns
            listOf(robot1, robot3, robot6)

        // when
        // now Robot 4 dies
        robotApplicationService.executeAttacks(fightingCommands)

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

    @Test
    fun `All MineCommands ran successfully`() {
        // given
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf()

        every { gameMapMockService.getResourceOnPlanet(robot1.planet.planetId) } returns ResourceType.COAL
        every { gameMapMockService.getResourceOnPlanet(robot2.planet.planetId) } returns ResourceType.IRON

        every { gameMapMockService.mine(robot1.planet.planetId, robot1.miningSpeed) } returns robot1.miningSpeed
        every { gameMapMockService.mine(robot2.planet.planetId, robot2.miningSpeed) } returns robot2.miningSpeed

        robot2.upgrade(UpgradeType.MINING, 1)

        val mineCommands = listOf(
            MineCommand(robot1.id, UUID.randomUUID()),
            MineCommand(robot2.id, UUID.randomUUID()),
        )

        // when
        robotApplicationService.executeMining(mineCommands)
        // then
        assertEquals(robot1.miningSpeed, robot1.inventory.getStorageUsageForResource(ResourceType.COAL))
        assertEquals(robot2.miningSpeed, robot2.inventory.getStorageUsageForResource(ResourceType.IRON))
    }

    @Test
    fun `Mining doesn't work for robot2 because no resource is available on the planet`() {
        // given
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf()

        every { gameMapMockService.getResourceOnPlanet(robot1.planet.planetId) } returns ResourceType.COAL
        every { gameMapMockService.getResourceOnPlanet(robot2.planet.planetId) } throws
            NoResourceOnPlanetException(robot2.planet.planetId)

        every { gameMapMockService.mine(robot1.planet.planetId, robot1.miningSpeed) } returns robot1.miningSpeed
        every { gameMapMockService.mine(robot2.planet.planetId, robot2.miningSpeed) } returns robot2.miningSpeed

        justRun { eventSender.handleException(any(), any()) }

        val mineCommands = listOf(
            MineCommand(robot1.id, UUID.randomUUID()),
            MineCommand(robot2.id, UUID.randomUUID()),
        )

        // when
        robotApplicationService.executeMining(mineCommands)

        // then
        verify(exactly = 1) {
            eventSender.handleException(any(), any())
        }
        assertEquals(robot1.miningSpeed, robot1.inventory.getStorageUsageForResource(ResourceType.COAL))
    }

    @Test
    fun `Resources get distributed evenly because planet ran out of resources`() {
        // given
        // given
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf()

        every { gameMapMockService.getResourceOnPlanet(robot1.planet.planetId) } returns ResourceType.COAL

        robot1.upgrade(UpgradeType.MINING_SPEED, 1)
        robot1.upgrade(UpgradeType.MINING_SPEED, 2) // corresponds to 10
        robot3.upgrade(UpgradeType.MINING_SPEED, 1)
        robot3.upgrade(UpgradeType.MINING_SPEED, 2)
        robot3.upgrade(UpgradeType.MINING_SPEED, 3) // corresponds to 15
        robot4.upgrade(UpgradeType.MINING_SPEED, 1)
        robot4.upgrade(UpgradeType.MINING_SPEED, 2)
        robot4.upgrade(UpgradeType.MINING_SPEED, 3)
        robot4.upgrade(UpgradeType.MINING_SPEED, 4) // corresponds to 20

        val amount = robot1.miningSpeed + robot3.miningSpeed + robot4.miningSpeed
        assertEquals(45, amount) // make sure upgrading isn't the problem
        every { gameMapMockService.mine(robot1.planet.planetId, amount) } returns 18

        val mineCommands = listOf(
            MineCommand(robot1.id, UUID.randomUUID()),
            MineCommand(robot3.id, UUID.randomUUID()),
            MineCommand(robot4.id, UUID.randomUUID()),
        )

        // when
        robotApplicationService.executeMining(mineCommands)

        // then
        assertEquals(4, robot1.inventory.getStorageUsageForResource(ResourceType.COAL))
        assertEquals(6, robot3.inventory.getStorageUsageForResource(ResourceType.COAL))
        assertEquals(8, robot4.inventory.getStorageUsageForResource(ResourceType.COAL))
    }

    @Test
    fun `Resources get distributed as fairly as possible after mining`() {
        // given
        // given
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf()

        every { gameMapMockService.getResourceOnPlanet(robot1.planet.planetId) } returns ResourceType.COAL

        robot1.upgrade(UpgradeType.MINING_SPEED, 1)
        robot1.upgrade(UpgradeType.MINING_SPEED, 2) // corresponds to 10
        robot3.upgrade(UpgradeType.MINING_SPEED, 1)
        robot3.upgrade(UpgradeType.MINING_SPEED, 2)
        robot3.upgrade(UpgradeType.MINING_SPEED, 3) // corresponds to 15
        robot4.upgrade(UpgradeType.MINING_SPEED, 1)
        robot4.upgrade(UpgradeType.MINING_SPEED, 2)
        robot4.upgrade(UpgradeType.MINING_SPEED, 3)
        robot4.upgrade(UpgradeType.MINING_SPEED, 4) // corresponds to 20

        val amount = robot1.miningSpeed + robot3.miningSpeed + robot4.miningSpeed
        assertEquals(45, amount) // make sure upgrading isn't the problem
        every { gameMapMockService.mine(robot1.planet.planetId, amount) } returns 22

        val mineCommands = listOf(
            MineCommand(robot1.id, UUID.randomUUID()),
            MineCommand(robot3.id, UUID.randomUUID()),
            MineCommand(robot4.id, UUID.randomUUID()),
        )

        // when
        robotApplicationService.executeMining(mineCommands)

        // then
        assertEquals(5, robot1.inventory.getStorageUsageForResource(ResourceType.COAL))
        assertEquals(7, robot3.inventory.getStorageUsageForResource(ResourceType.COAL))
        assertEquals(10, robot4.inventory.getStorageUsageForResource(ResourceType.COAL))
    }

    @Test
    fun `Invalid MineCommands or problems with map service don't affect the valid commands, but get handled`() {
        // given
        val unknownPlanetId = UUID.randomUUID()
        robot5.move(Planet(unknownPlanetId), 0)
        val newPlanetId = UUID.randomUUID()
        robot6.move(Planet(newPlanetId), 0)
        val anotherPlanetId = UUID.randomUUID()
        robot3.move(Planet(anotherPlanetId), 0)

        every { robotRepository.findByIdOrNull(unknownRobotId) } returns null
        every { gameMapMockService.getResourceOnPlanet(robot3.planet.planetId) } throws
            NoResourceOnPlanetException(robot3.planet.planetId)
        every { gameMapMockService.getResourceOnPlanet(robot5.planet.planetId) } throws ClientException("")
        every { gameMapMockService.getResourceOnPlanet(robot4.planet.planetId) } returns ResourceType.PLATIN
        every { gameMapMockService.getResourceOnPlanet(robot6.planet.planetId) } returns ResourceType.COAL
        every { gameMapMockService.mine(robot6.planet.planetId, robot6.miningSpeed) } returns robot6.miningSpeed
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf() // we dont need the return value
        justRun { eventSender.handleException(any(), any()) }

        val mineCommands = listOf(
            MineCommand(unknownRobotId, UUID.fromString("11111111-1111-1111-1111-111111111111")), // unknown robot
            MineCommand(robot3.id, UUID.fromString("33333333-3333-3333-3333-33333333333")), // planet has no resource
            MineCommand(robot5.id, UUID.fromString("55555555-5555-5555-5555-55555555555")), // problem with map service
            MineCommand(robot4.id, UUID.fromString("44444444-4444-4444-4444-44444444444")), // MiningLevel too low
            MineCommand(robot6.id, UUID.fromString("66666666-6666-6666-6666-66666666666")) // valid
        )

        // when
        robotApplicationService.executeMining(mineCommands)

        // then
        verify(exactly = 4) {
            eventSender.handleException(any(), any())
        }
        assertEquals(robot6.miningSpeed, robot6.inventory.getStorageUsageForResource(ResourceType.COAL))
    }

    @Test
    fun `Robots spawn correctly`() {
        // given
        val slot = CapturingSlot<Robot>()
        every { robotRepository.save(capture(slot)) } returns robot1 // we don't care about the return value, only the capture
        // when
        robotApplicationService.spawn(player1Id, planet1.planetId)
        // then
        assertEquals(player1Id, slot.captured.player)
        assertEquals(planet1.planetId, slot.captured.planet.planetId)
    }

    @Test
    fun `UseAttackItems correctly deals damage according to the commands`() {
        // given
        every { robotRepository.save(any()) } returns robot1
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf()
        every { robotRepository.findAllByPlanet_PlanetId(planet2.planetId) } returns listOf(robot2, robot5)
        every { robotRepository.findAllByPlanet_PlanetId(planet1.planetId) } returns
            listOf(robot1, robot3, robot4, robot6)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(planet2.planetId) } returns
            listOf(robot2, robot5)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(planet1.planetId) } returns
            listOf(robot3, robot4, robot6)
        justRun { robotRepository.delete(any()) }

        for (i in 1..5) robot1.upgrade(UpgradeType.HEALTH, i)

        robot1.inventory.addItem(AttackItemType.ROCKET)
        robot2.inventory.addItem(AttackItemType.NUKE)
        robot3.inventory.addItem(AttackItemType.ROCKET)
        robot4.inventory.addItem(AttackItemType.SELF_DESTRUCTION)
        robot5.inventory.addItem(AttackItemType.LONG_RANGE_BOMBARDMENT)

        val commands = listOf(
            FightingItemUsageCommand(robot1.id, AttackItemType.ROCKET, robot1.id, UUID.randomUUID()),
            FightingItemUsageCommand(robot2.id, AttackItemType.NUKE, planet2.planetId, UUID.randomUUID()),
            FightingItemUsageCommand(robot3.id, AttackItemType.ROCKET, robot4.id, UUID.randomUUID()),
            FightingItemUsageCommand(robot4.id, AttackItemType.SELF_DESTRUCTION, robot4.id, UUID.randomUUID()),
            FightingItemUsageCommand(
                robot5.id, AttackItemType.LONG_RANGE_BOMBARDMENT,
                robot6.planet.planetId, UUID.randomUUID()
            )
        )

        // when
        robotApplicationService.executeCommands(commands)

        // then
        verify(exactly = 0) { eventSender.handleException(any(), any()) }
        assertAll(
            {
                assert(robot1.health == robot1.maxHealth - 5 - 20 - 10)
            },
            {
                assert(robot2.health < 0)
                assert(!robot2.alive)
            },
            {
                assert(robot3.health < 0)
                assert(!robot3.alive)
            },
            {
                assert(robot4.health < 0)
                assert(!robot4.alive)
            },
            {
                assert(robot5.health < 0)
                assert(!robot5.alive)
            },
            {
                assert(robot6.health < 0)
                assert(!robot6.alive)
            }
        )
    }

    @Test
    fun `UseAttackitems distributes remaining resources`() {
        // given
        every { robotRepository.save(any()) } returns robot1
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf()
        every { robotRepository.findAllByPlanet_PlanetId(planet2.planetId) } returns listOf(robot2, robot5)
        every { robotRepository.findAllByPlanet_PlanetId(planet1.planetId) } returnsMany
            listOf(
                listOf(robot1, robot3, robot4, robot6),
                listOf(robot1, robot3, robot4, robot6),
                listOf(robot1)
            )
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(planet2.planetId) } returns
            listOf(robot2, robot5)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(planet1.planetId) } returns
            listOf(robot3, robot4, robot6)
        justRun { robotRepository.delete(any()) }

        for (i in 1..5) robot1.upgrade(UpgradeType.HEALTH, i)

        robot1.inventory.addItem(AttackItemType.ROCKET)
        robot2.inventory.addItem(AttackItemType.NUKE)
        robot3.inventory.addItem(AttackItemType.ROCKET)
        robot4.inventory.addItem(AttackItemType.SELF_DESTRUCTION)
        robot5.inventory.addItem(AttackItemType.LONG_RANGE_BOMBARDMENT)

        robot3.inventory.addResource(ResourceType.GOLD, 5)
        robot4.inventory.addResource(ResourceType.PLATIN, 10)
        robot6.inventory.addResource(ResourceType.COAL, 10)

        val commands = listOf(
            FightingItemUsageCommand(robot1.id, AttackItemType.ROCKET, robot1.id, UUID.randomUUID()),
            FightingItemUsageCommand(robot2.id, AttackItemType.NUKE, planet2.planetId, UUID.randomUUID()),
            FightingItemUsageCommand(robot3.id, AttackItemType.ROCKET, robot4.id, UUID.randomUUID()),
            FightingItemUsageCommand(robot4.id, AttackItemType.SELF_DESTRUCTION, robot4.id, UUID.randomUUID()),
            FightingItemUsageCommand(
                robot5.id, AttackItemType.LONG_RANGE_BOMBARDMENT,
                robot6.planet.planetId, UUID.randomUUID()
            )
        )

        // when
        robotApplicationService.executeFightingItemUsageCommand(commands)

        // then
        assertAll(
            {
                assert(robot1.inventory.getStorageUsageForResource(ResourceType.COAL) == 5)
            },
            {
                assert(robot1.inventory.getStorageUsageForResource(ResourceType.PLATIN) == 10)
            },
            {
                assert(robot1.inventory.getStorageUsageForResource(ResourceType.GOLD) == 5)
            }
        )
    }
}

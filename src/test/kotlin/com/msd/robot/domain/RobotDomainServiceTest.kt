package com.msd.robot.domain

import com.msd.application.GameMapPlanetDto
import com.msd.application.GameMapService
import com.msd.domain.ResourceType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.ReparationItemType
import com.msd.planet.domain.Planet
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import java.util.*

@ExtendWith(MockKExtension::class)
internal class RobotDomainServiceTest {

    lateinit var robot1: Robot
    lateinit var robot2: Robot
    lateinit var robot3: Robot
    lateinit var robot4: Robot
    lateinit var robot5: Robot
    lateinit var robot6: Robot

    lateinit var player1Id: UUID
    lateinit var player2Id: UUID

    lateinit var planet1: Planet
    lateinit var planet2: Planet

    lateinit var robots: List<Robot>

    @MockK
    lateinit var robotRepository: RobotRepository

    @MockK
    lateinit var gameMapService: GameMapService

    lateinit var robotDomainService: RobotDomainService

    @BeforeEach
    fun setup() {
        robotDomainService = RobotDomainService(robotRepository, gameMapService)
        player1Id = UUID.randomUUID()
        player2Id = UUID.randomUUID()

        planet1 = Planet(UUID.randomUUID())
        planet2 = Planet(UUID.randomUUID())

        robot1 = Robot(player1Id, planet1)
        robot2 = Robot(player1Id, planet1)
        robot3 = Robot(player1Id, planet2)
        robot4 = Robot(player2Id, planet1)
        robot5 = Robot(player2Id, planet1)
        robot6 = Robot(player2Id, planet2)
        robots = listOf(robot1, robot2, robot3, robot4, robot5, robot6)
    }

    @Test
    fun `Robots can't attack Robots in another System`() {
        // given
        every { robotRepository.findByIdOrNull(any()) } answers { robots.find { it.id == firstArg() } }

        // when
        assertThrows<OutOfReachException>("Robots must be on the same Planet to attack each other") {
            robotDomainService.fight(robot1, robot6)
        }
        // then
        assertEquals(10, robot6.health)
        assertEquals(20, robot1.energy)
    }

    @Test
    fun `Robots can attack destroyed Robots`() {
        // given
        every { robotRepository.findByIdOrNull(any()) } answers { robots.find { it.id == firstArg() } }
        every { robotRepository.save(robot1) } returns robot1
        every { robotRepository.save(robot2) } returns robot2
        every { robotRepository.save(robot4) } returns robot4

        for (i in 1..5) robotDomainService.fight(robot1, robot4)
        for (i in 1..5) robotDomainService.fight(robot2, robot4)
        assert(!robot4.alive)

        // when
        robotDomainService.fight(robot1, robot4)
        robotDomainService.fight(robot2, robot4)
        // then
        assertEquals(-2, robot4.health)
        assertAll(
            {
                assertEquals(14, robot1.energy)
            },
            {
                assertEquals(14, robot2.energy)
            }
        )
    }

    @Test
    fun `Destroyed Robots can attack other Robots`() {
        // given
        every { robotRepository.findByIdOrNull(any()) } answers { robots.find { it.id == firstArg() } }
        every { robotRepository.save(robot1) } returns robot1
        every { robotRepository.save(robot4) } returns robot4

        for (i in 1..10) robotDomainService.fight(robot1, robot4)
        assert(!robot4.alive)

        // when
        robotDomainService.fight(robot4, robot1)
        // then
        assertEquals(9, robot1.health)
        assertEquals(19, robot4.energy)
    }

    @Test
    fun `Robots need enough energy to attack`() {
        // given
        robot1.move(planet2, 20)
        every { robotRepository.findByIdOrNull(any()) } answers { robots.find { it.id == firstArg() } }

        // when
        assertThrows<NotEnoughEnergyException> {
            robotDomainService.fight(robot1, robot6)
        }
        // then
        assertEquals(10, robot6.health)
        assertEquals(0, robot1.energy)
    }

    @Test
    fun `post clean fight distributes all resources equally when possible`() {
        // given
        ResourceType.values().forEach {
            robot1.inventory.addResource(it, 3)
        }
        every { robotRepository.findAllByPlanet_PlanetId(planet1.planetId) } returns
            listOf(robot2, robot4, robot5)
        every { robotRepository.findAllByAliveFalseAndPlanet_PlanetId(planet1.planetId) } returns listOf(robot1)
        every { robotRepository.findByIdOrNull(any()) } answers { robots.find { it.id == firstArg() } }
        justRun { robotRepository.delete(robot1) }
        every { robotRepository.saveAll(listOf(robot2, robot4, robot5)) } answers { firstArg() }
        // when
        for (i in 1..4) {
            robot2.attack(robot1)
            robot4.attack(robot1)
            robot5.attack(robot1)
        }
        assert(!robot1.alive) { "except robot1 to be dead" }

        robotDomainService.postFightCleanup(planet1.planetId)
        // then
        assertEquals(0, robot1.inventory.usedStorage)
        assertAll(
            "All robots have used storage of 5",
            { assertEquals(5, robot2.inventory.usedStorage) },
            { assertEquals(5, robot4.inventory.usedStorage) },
            { assertEquals(5, robot5.inventory.usedStorage) }

        )
        assertAll(
            "all robots have one of each resource",
            ResourceType.values().map {
                {
                    assertEquals(1, robot2.inventory.getStorageUsageForResource(it))
                    assertEquals(1, robot4.inventory.getStorageUsageForResource(it))
                    assertEquals(1, robot5.inventory.getStorageUsageForResource(it))
                }
            }
        )
    }

    @Test
    fun `using repair swarm heals all robots of the player owning the robot`() {
        // given
        robot3.move(planet2, 0)
        robot1.inventory.addItem(ReparationItemType.REPARATION_SWARM)
        val player1Robots = listOf(robot1, robot2, robot3)
        player1Robots.forEach {
            it.upgrade(UpgradeType.HEALTH)
            it.repair()
            it.receiveDamage(21)
        }

        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every {
            robotRepository.findAllByPlayerAndPlanet_PlanetId(
                robot1.player,
                robot1.planet.planetId
            )
        } returns player1Robots
        every { robotRepository.saveAll(player1Robots) } returns player1Robots

        // when
        robotDomainService.useReparationItem(robot1.id, robot1.player, ReparationItemType.REPARATION_SWARM)

        // then
        assertAll(
            "all robots of player1 healed for 20 health",
            robots.filter { it.player == player1Id }.map { { assertEquals(24, it.health) } }
        )
        assertEquals(0, robot1.inventory.getItemAmountByType(ReparationItemType.REPARATION_SWARM))
    }

    @Test
    fun `using a wormhole moves a robot`() {
        // given
        val planetDTO = GameMapPlanetDto(UUID.randomUUID(), 3)

        robot1.inventory.addItem(MovementItemType.WORMHOLE)

        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.save(robot1) } returns robot1

        every { gameMapService.getAllPlanets() } returns listOf(planetDTO)

        // when
        robotDomainService.useMovementItem(robot1.id, robot1.player, MovementItemType.WORMHOLE)

        // then
        assertEquals(planetDTO.id, robot1.planet.planetId)
        assertEquals(0, robot1.inventory.getItemAmountByType(MovementItemType.WORMHOLE))
    }
}

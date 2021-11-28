package com.msd.item.domain

import com.msd.domain.InvalidTargetException
import com.msd.planet.domain.Planet
import com.msd.robot.application.exception.RobotNotFoundException
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.exception.TargetRobotOutOfReachException
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import java.util.*

@ExtendWith(MockKExtension::class)
class AttackItemTypeTest {

    lateinit var robot1: Robot
    lateinit var robot2: Robot
    lateinit var robot3: Robot
    lateinit var robot4: Robot
    lateinit var robot5: Robot
    lateinit var robot6: Robot

    val planet1 = Planet(UUID.randomUUID())
    val planet2 = Planet(UUID.randomUUID())
    val planet3 = Planet(UUID.randomUUID())

    @MockK
    lateinit var robotRepository: RobotRepository

    val player1 = UUID.randomUUID()
    val player2 = UUID.randomUUID()
    val player3 = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        robot1 = Robot(player1, planet1)
        robot2 = Robot(player1, planet2)
        robot3 = Robot(player2, planet1)
        robot4 = Robot(player2, planet2)
        robot5 = Robot(player3, planet3)
        robot6 = Robot(player3, planet1)

        // We dont use returns of save calls
        every { robotRepository.save(any()) } returns robot1
        every { robotRepository.saveAll(any<List<Robot>>()) } returns listOf()
    }

    @Test
    fun `Using a rocket item deals the corresponding damage to the target robot`() {
        // given
        every { robotRepository.findByIdOrNull(robot3.id) } returns robot3

        // when
        AttackItemType.ROCKET.use(robot1, robot3.id, robotRepository)

        // then
        assert(robot3.health == robot3.maxHealth - 5)
    }

    @Test
    fun `Cannot use rocket item for target on different planet`() {
        // given
        every { robotRepository.findByIdOrNull(robot4.id) } returns robot4

        // then
        assertThrows<TargetRobotOutOfReachException>("The target robot is not on the same planet as the using robot") {
            AttackItemType.ROCKET.use(robot1, robot4.id, robotRepository)
        }
    }

    @Test
    fun `Cannot use rocket item on unknown robot`() {
        // given
        every { robotRepository.findByIdOrNull(any()) } returns null

        // then
        assertThrows<RobotNotFoundException>("The rocket couldn't find a robot with that UUID") {
            AttackItemType.ROCKET.use(robot1, UUID.randomUUID(), robotRepository)
        }
    }

    @Test
    fun `Using a rocket item returns the correct battlefield`() {
        // given
        every { robotRepository.findByIdOrNull(robot3.id) } returns robot3

        // when
        val battlefield = AttackItemType.ROCKET.use(robot1, robot3.id, robotRepository)

        // then
        assert(battlefield == robot1.planet.planetId)
    }

    @Test
    fun `Long Range Bombardment deals corresponding damage to every robot on the target planet`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(planet2.planetId) } returns listOf(robot2, robot4)

        // when
        AttackItemType.LONG_RANGE_BOMBARDMENT.use(robot1, planet2.planetId, robotRepository)

        // then
        assertAll(
            {
                assert(robot4.health == robot4.maxHealth - 10)
            },
            {
                assert(robot2.health == robot2.maxHealth - 10)
            },
            { // does not deal damage to robots on other planets
                assert(
                    robot1.health + robot3.health + robot5.health ==
                        robot1.maxHealth + robot3.maxHealth + robot5.maxHealth
                )
            }
        )
    }

    @Test
    fun `Long Range Bombardment returns the correct battlefield`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(planet2.planetId) } returns listOf(robot3, robot4)

        // when
        val battlefield = AttackItemType.LONG_RANGE_BOMBARDMENT.use(robot1, planet2.planetId, robotRepository)

        // then
        assert(battlefield == planet2.planetId)
    }

    @Test
    fun `Using self destruct item destroys the robot using it`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns listOf(robot1, robot3, robot6)

        // when
        AttackItemType.SELF_DESTRUCTION.use(robot1, robot1.id, robotRepository)

        // then
        assert(!robot1.alive)
    }

    @Test
    fun `Using self destruct deals corresponding damage to all robots on the same planet as the using robot`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns listOf(robot1, robot3, robot6)

        // when
        AttackItemType.SELF_DESTRUCTION.use(robot1, robot1.id, robotRepository)

        // then
        assertAll(
            {
                assert(robot3.health == robot3.maxHealth - 20)
            },
            {
                assert(robot6.health == robot6.maxHealth - 20)
            },
            { // dont deal damage to robots on other planets
                assert(
                    robot2.health + robot4.health + robot5.health ==
                        robot2.maxHealth + robot4.maxHealth + robot5.maxHealth
                )
            }
        )
    }

    @Test
    fun `Using self destruct item returns the correct battlefield`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(robot1.planet.planetId) } returns listOf(robot1, robot3, robot6)

        // when
        val battlefield = AttackItemType.SELF_DESTRUCTION.use(robot1, robot1.id, robotRepository)

        // then
        assert(battlefield == robot1.planet.planetId)
    }

    @Test
    fun `Self destruct cannot be issued for another robot than the item carrier`() {
        // when
        assertThrows<InvalidTargetException>("Robot cannot self-destruct other robot than itself") {
            AttackItemType.SELF_DESTRUCTION.use(robot1, robot1.planet.planetId, robotRepository)
        }
    }

    @Test
    fun `Using nuke deals corresponding damage to all robots on the target planet`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(robot2.planet.planetId) } returns listOf(robot2, robot4)

        // when
        AttackItemType.NUKE.use(robot1, robot2.planet.planetId, robotRepository)

        // then
        assertAll(
            {
                assert(robot2.health == robot2.maxHealth - 100)
            },
            {
                assert(robot4.health == robot4.maxHealth - 100)
            }
        )
    }

    @Test
    fun `Using nuke returns the correct battlefield`() {
        // given
        every { robotRepository.findAllByPlanet_PlanetId(robot2.planet.planetId) } returns listOf(robot2, robot4)

        // when
        val battlefield = AttackItemType.NUKE.use(robot1, robot2.planet.planetId, robotRepository)

        // then
        assert(battlefield == robot2.planet.planetId)
    }
}

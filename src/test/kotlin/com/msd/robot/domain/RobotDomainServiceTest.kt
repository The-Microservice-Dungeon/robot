package com.msd.robot.domain

import com.msd.planet.domain.Planet
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
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

    @MockK
    lateinit var robotRepository: RobotRepository

    lateinit var robotDomainService: RobotDomainService

    @BeforeEach
    fun setup() {
        robotDomainService = RobotDomainService(robotRepository)
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
    }

    // TODO do we batch attacks in the domain service or in the application Service?
    fun `Robots can't attack Robots in another System`() {
        // given
        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.findByIdOrNull(robot6.id) } returns robot6

        // when
        assertThrows<InvalidAttackException>("Robots must be on the same Planet to attack each other") {
            robotDomainService.attackRobot(robot1.id, robot6.id)
        }
        // then
        assertEquals(10, robot6.health)
        assertEquals(20, robot1.energy)
    }

    fun `Robots can attack destroyed Robots`() {
        // given
        for (i in 0..5) robotDomainService.attackRobot(robot1.id, robot4.id)
        for (i in 0..5) robotDomainService.attackRobot(robot2.id, robot4.id)
        assert(!robot4.alive)

        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.findByIdOrNull(robot2.id) } returns robot2
        every { robotRepository.findByIdOrNull(robot4.id) } returns robot4
        // when
        robotDomainService.attackRobot(robot1.id, robot4.id)
        robotDomainService.attackRobot(robot2.id, robot4.id)
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

    fun `Destroyed Robots can attack other Robots`() {
        // given
        for (i in 0..10) robotDomainService.attackRobot(robot1.id, robot4.id)
        assert(!robot4.alive)

        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.findByIdOrNull(robot4.id) } returns robot4
        // when
        robotDomainService.attackRobot(robot4.id, robot1.id)
        // then
        assertEquals(9, robot1.health)
        assertEquals(19, robot2.energy)
    }

    fun `Robots need enough energy to attack`() {
        // given
        robot1.move(planet2, 20)

        every { robotRepository.findByIdOrNull(robot1.id) } returns robot1
        every { robotRepository.findByIdOrNull(robot6.id) } returns robot6
        // when
        assertThrows<NotEnoughEnergyException> {
            robotDomainService.attackRobot(robot1.id, robot6.id)
        }
        // then
        assertEquals(10, robot6.health)
        assertEquals(0, robot1.energy)
    }
}
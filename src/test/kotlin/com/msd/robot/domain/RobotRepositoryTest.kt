package com.msd.robot.domain

import com.msd.planet.domain.Planet
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.util.*
import javax.persistence.EntityNotFoundException

@SpringBootTest
class RobotRepositoryTest(
    @Autowired val robotRepository: RobotRepository
) {

    @Test
    fun `Successfully saves the planet of a robot`() {
        val planet = Planet(UUID.randomUUID())
        val robot1 = Robot(UUID.randomUUID(), planet)
        val savedRobot1 = robotRepository.save(robot1)
        assert(!savedRobot1.planet.blocked)

        val robot2 = Robot(UUID.randomUUID(), planet)
        robot2.block()
        val savedRobot2 = robotRepository.save(robot2)
        assert(savedRobot2.planet.blocked)

        val fetchedRobot1 = robotRepository.findByIdOrNull(robot1.id)
            ?: throw EntityNotFoundException("Robot not found")
        assert(fetchedRobot1.planet.blocked)
    }
}

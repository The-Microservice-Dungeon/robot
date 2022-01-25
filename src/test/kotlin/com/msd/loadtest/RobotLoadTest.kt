package com.msd.loadtest

import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import java.util.*
import javax.transaction.Transactional
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@SpringBootTest
@Transactional
@ActiveProfiles(profiles = ["test"])
class RobotLoadTest(
    @Autowired
    private val robotRepository: RobotRepository
) {

    val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalTime::class)
    @Test
    fun `write 100 robots to the repo and retrieve one`() {
        val robot = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robotRepository.save(robot)
        for (i in 1..99) {
            robotRepository.save(Robot(UUID.randomUUID(), Planet(UUID.randomUUID())))
        }

        val robots = robotRepository.findAll().toList()
        Assertions.assertEquals(100, robots.size)

        var repoRobot: Robot? = null
        val timePassed: Duration = measureTime {
            repoRobot = robotRepository.findByIdOrNull(robot.id)!!
        }
        logger.info("$timePassed has passed while looking for the specified Robot")
        assertNotNull(repoRobot)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `write 1000 robots to the repo and retrieve one`() {
        val robot = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robotRepository.save(robot)
        for (i in 1..999) {
            robotRepository.save(Robot(UUID.randomUUID(), Planet(UUID.randomUUID())))
        }

        val robots = robotRepository.findAll().toList()
        Assertions.assertEquals(1000, robots.size)

        var repoRobot: Robot? = null
        val timePassed: Duration = measureTime {
            repoRobot = robotRepository.findByIdOrNull(robot.id)!!
        }
        logger.info("$timePassed has passed while looking for the specified Robot")
        assertNotNull(repoRobot)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `write 10000 robots to the repo and retrieve one`() {
        val robot = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robotRepository.save(robot)
        for (i in 1..9999) {
            robotRepository.save(Robot(UUID.randomUUID(), Planet(UUID.randomUUID())))
        }

        val robots = robotRepository.findAll().toList()
        Assertions.assertEquals(10000, robots.size)

        var repoRobot: Robot? = null
        val timePassed: Duration = measureTime {
            repoRobot = robotRepository.findByIdOrNull(robot.id)!!
        }
        logger.info("$timePassed has passed while looking for the specified Robot")
        assertNotNull(repoRobot)
    }
}

package com.msd.robot.domain

import com.msd.planet.domain.Planet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityNotFoundException

@SpringBootTest
@ActiveProfiles(profiles = ["test"])
@Transactional
@DirtiesContext
class RobotRepositoryTest(
    @Autowired val robotRepository: RobotRepository
) {
    // TODO fix these tests not running when they're not the first. Some tests drops the database and these tests don't pass anymore since there is no database
    val player1Id: UUID = UUID.randomUUID()
    val player2Id: UUID = UUID.randomUUID()

    lateinit var planet1: Planet
    lateinit var planet2: Planet

    lateinit var robot1: Robot
    lateinit var robot2: Robot
    lateinit var robot3: Robot
    lateinit var robot4: Robot
    lateinit var robot5: Robot
    lateinit var robot6: Robot

    @BeforeEach
    fun setup() {
        planet1 = Planet(UUID.randomUUID())
        planet2 = Planet(UUID.randomUUID())

        robot1 = Robot(player1Id, planet1)
        robot2 = Robot(player1Id, planet2)
        robot3 = Robot(player1Id, planet1)
        robot4 = Robot(player2Id, planet1)
        robot5 = Robot(player2Id, planet2)
        robot6 = Robot(player2Id, planet1)

        robotRepository.saveAll(arrayListOf(robot1, robot2, robot3, robot4, robot5, robot6))
    }

    @Test
    fun `Saved Robots can be retrieved`() {
        // when
        robotRepository.save(robot1)

        // then
        assertEquals(robot1.id, robotRepository.findByIdOrNull(robot1.id)!!.id)
    }

    @Test
    fun `The Blocked Status of a planet can be viewed from every robot`() {
        // given
        robot1.block()
        robotRepository.save(robot1)

        // then
        val fetchedRobot3 = robotRepository.findByIdOrNull(robot3.id)
            ?: throw EntityNotFoundException("Robot not found")

        // then
        assert(fetchedRobot3.planet.blocked)
    }

    @Test
    fun `Retrieves the dead robots on a planet`() {
        // given
        robot1.alive = false
        robot3.alive = false

        robotRepository.save(robot1)
        robotRepository.save(robot3)

        // when
        val robotIds = robotRepository.findAllByAliveFalseAndPlanet_PlanetId(planet1.planetId).map { it.id }

        // then
        assertAll(
            {
                assert(robotIds.contains(robot1.id))
            },
            {
                assert(robotIds.contains(robot3.id))
            }
        )
    }

    @Test
    fun `FindAllByPlanet_PlanetId returns all robots on the planet`() {
        val robotIds = robotRepository.findAllByPlanet_PlanetId(planet1.planetId).map { it.id }
        assertAll(
            {
                assert(robotIds.contains(robot1.id))
            },
            {
                assert(robotIds.contains(robot3.id))
            },
            {
                assert(robotIds.contains(robot4.id))
            },
            {
                assert(robotIds.contains(robot6.id))
            }
        )
    }
}

package com.msd.robot.domain

import com.msd.domain.ResourceType
import com.msd.robot.application.InvalidPlayerException
import com.msd.robot.application.RobotNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class RobotDomainService(
    val robotRepository: RobotRepository
) {

    fun fight(attacker: Robot, target: Robot) {
        if (attacker.planet.planetId != target.planet.planetId)
            throw OutOfReachException("The attacking robot and the defending robot are not on the same planet")

        attacker.attack(target)

        saveRobot(attacker)
        saveRobot(target)
    }

    fun postFightCleanup(planetId: UUID) {
        val deadRobotsOnPlanet = robotRepository.findAllByAliveFalseAndPlanet_PlanetId(planetId)
        val resourcesToBeDistributed = mutableMapOf(
            ResourceType.COAL to 0,
            ResourceType.IRON to 0,
            ResourceType.GEM to 0,
            ResourceType.GOLD to 0,
            ResourceType.PLATIN to 0,
        )

        deadRobotsOnPlanet.forEach { robot ->
            ResourceType.values().forEach { resourceType ->
                resourcesToBeDistributed[resourceType] =
                    resourcesToBeDistributed[resourceType]!!.plus(robot.inventory.takeAllOfResource(resourceType))
            }
            robotRepository.delete(robot)
        }

        val robotsAliveOnPlanet = robotRepository.findAllByPlanet_PlanetId(planetId)
        var numAliveAndFreeInventory = robotsAliveOnPlanet
            .filter { it.inventory.usedStorage <= it.inventory.maxStorage }
            .count()

        // reversed, so the most valuable resources get distributed first
        ResourceType.values().reversed().forEach { resource ->
            while (resourcesToBeDistributed[resource]!! > 0 && numAliveAndFreeInventory > 0) {
                val resourcesPerRobot = resourcesToBeDistributed[resource]!!.floorDiv(numAliveAndFreeInventory)

                if (resourcesPerRobot >= 1) {
                    robotsAliveOnPlanet.forEach { robot ->
                        val freeInventory = robot.inventory.maxStorage - robot.inventory.usedStorage
                        resourcesToBeDistributed[resource] =
                            if (freeInventory < resourcesPerRobot) {
                                numAliveAndFreeInventory -= 1
                                robot.inventory.addResource(resource, freeInventory)
                                resourcesToBeDistributed[resource]!! - freeInventory
                            } else {
                                robot.inventory.addResource(resource, resourcesPerRobot)
                                resourcesToBeDistributed[resource]!! - resourcesPerRobot
                            }
                    }
                } else {
                    // shuffled to make sure its not always the same robots getting the last resources
                    robotsAliveOnPlanet.shuffled().forEach { robot ->
                        if (resourcesToBeDistributed[resource]!! > 0 && robot.inventory.maxStorage - robot.inventory.usedStorage > 0) {
                            resourcesToBeDistributed[resource] = resourcesToBeDistributed[resource]!! - 1
                            robot.inventory.addResource(resource, 1)
                        }
                    }
                }
            }
        }
        saveAll(robotsAliveOnPlanet)
    }

    /**
     * Checks if the specified `playerId` is the owner of the specified [Robot].
     *
     * @param robot      the `Robot` whose ownership will be checked
     * @param playerId   the `playerUUID` which should be checked against the `Robot`
     * @throws InvalidPlayerException if the Robot's ID doesn't match the specified ID
     */
    fun checkRobotBelongsToPlayer(robot: Robot, playerId: UUID) {
        if (robot.player != playerId) throw InvalidPlayerException("Specified player doesn't match player specified in robot")
    }

    /**
     * Gets the specified [Robot].
     *
     * @param robotId the `UUID` of the robot which should be returned
     * @return the specified Robot
     * @throws RobotNotFoundException  if there is no `Robot` with the specified ID
     */
    fun getRobot(robotId: UUID): Robot {
        return robotRepository.findByIdOrNull(robotId)
            ?: throw RobotNotFoundException("Can't find robot with id $robotId")
    }

    /**
     * Saves the specified Robot.
     *
     * @param robot   the `Robot` which should be saved
     * @return the saved Robot
     */
    fun saveRobot(robot: Robot): Robot {
        return robotRepository.save(robot)
    }

    fun saveAll(robots: List<Robot>): List<Robot> {
        return robotRepository.saveAll(robots).toList()
    }
}

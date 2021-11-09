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

    /**
     * Check if an attack is valid, and if so, execute the attack.
     *
     * @param attacker      The robot attacking
     * @param target        The target of the attack
     * @throws OutOfReachException If the robots are not on the same planet
     */
    fun fight(attacker: Robot, target: Robot) {
        if (attacker.planet.planetId != target.planet.planetId)
            throw OutOfReachException("The attacking robot and the defending robot are not on the same planet")

        attacker.attack(target)

        saveRobot(attacker)
        saveRobot(target)
    }

    /**
     * Makes sure all resources of dead robots on the planet get distributed equally among all other robots on the
     * planet and that dead robots get deleted from the repository.
     *
     * This method should not throw any exceptions.
     */
    fun postFightCleanup(planetId: UUID) {
        val resourcesToBeDistributed = deleteDeadRobots(planetId)
        distributeResources(planetId, resourcesToBeDistributed)
    }

    /**
     * Find all the dead robots on the given planet and delete them. Return the accumulation of all their resources.
     *
     * @param planetId      The ID of the planet on which all dead robots should be deleted.
     */
    private fun deleteDeadRobots(planetId: UUID): MutableMap<ResourceType, Int> {
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
        return resourcesToBeDistributed
    }

    /**
     * Distribute the given resources among all the robots on the given planet.
     *
     * @param planetId      Id of the planet on which the resources get distributed. This is needed to determine which
     *                      robots get them.
     * @param resourcesToBeDistributed  The resources which need to get distributed.
     */
    private fun distributeResources(
        planetId: UUID,
        resourcesToBeDistributed: MutableMap<ResourceType, Int>
    ) {
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
        robotRepository.saveAll(robotsAliveOnPlanet)
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

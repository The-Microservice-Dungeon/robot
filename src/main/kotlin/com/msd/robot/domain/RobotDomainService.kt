package com.msd.robot.domain

import com.msd.domain.ResourceType
import com.msd.item.domain.ReparationItemType
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
        distributeDroppedResources(planetId, resourcesToBeDistributed)
    }

    /**
     * Find all the dead robots on the given planet and delete them. Return the accumulation of all their resources.
     *
     * @param planetId      The ID of the planet on which all dead robots should be deleted.
     * @return A MutableMap giving the amount of every resource that was dropped by the dead robots
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
                    resourcesToBeDistributed[resourceType]!!.plus(robot.inventory.takeAllResourcesOfType(resourceType))
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
    private fun distributeDroppedResources(
        planetId: UUID,
        resourcesToBeDistributed: MutableMap<ResourceType, Int>
    ) {
        val robotsAliveOnPlanet = robotRepository.findAllByPlanet_PlanetId(planetId)

        // reversed, so the most valuable resources get distributed first
        resourcesToBeDistributed.entries.sortedBy { it.key }.reversed().forEach {
            distributeDroppedResourcesOfTypeToRobots(it, robotsAliveOnPlanet)
        }
        robotRepository.saveAll(robotsAliveOnPlanet)
    }

    /**
     * Distributes all `Resources` of a given type to all specified `Robots`.
     *
     * @param MutableMap.MutableEntry the `MapEntry` specifying how many of which `ResourceType` wil be distributed
     */
    private fun distributeDroppedResourcesOfTypeToRobots(
        resourcesToBeDistributed: MutableMap.MutableEntry<ResourceType, Int>,
        robotsAliveOnPlanet: List<Robot>
    ) {
        while (resourcesToBeDistributed.value > 0 && getNumberOfRobotsWithUnusedStorage(robotsAliveOnPlanet) > 0) {
            val resourcesPerRobot =
                resourcesToBeDistributed.value.floorDiv(getNumberOfRobotsWithUnusedStorage(robotsAliveOnPlanet))

            if (resourcesPerRobot >= 1) {
                distributeResourcesEvenly(robotsAliveOnPlanet, resourcesToBeDistributed, resourcesPerRobot)
            } else {
                distributeResourcesShuffled(robotsAliveOnPlanet, resourcesToBeDistributed)
            }
        }
    }

    /**
     * Calculates the amount of [Robots] [Robot] which have unused storage.
     *
     * @return the amount of `Robots` which have unused storage as an Int
     */
    private fun getNumberOfRobotsWithUnusedStorage(robots: List<Robot>) =
        robots.count { !it.inventory.isFull() }

    /**
     * Distributes the specified amount of a `ResourceType` to each specified `Robot`.
     *
     * @param robotsAliveOnPlanet         the `Robots` which are alive and will get the specified `Resource`
     * @param resourcesToBeDistributed    the `ResourceType` which will be distributed and how many resources of this type there are
     * @param resourcesPerRobot           how many `Resources` each `Robot` gets
     */
    private fun distributeResourcesEvenly(
        robotsAliveOnPlanet: List<Robot>,
        resourcesToBeDistributed: MutableMap.MutableEntry<ResourceType, Int>,
        resourcesPerRobot: Int,
    ) {
        robotsAliveOnPlanet.forEach { robot ->
            val freeStorage = robot.inventory.maxStorage - robot.inventory.usedStorage
            resourcesToBeDistributed.setValue(
                resourcesToBeDistributed.value - if (freeStorage < resourcesPerRobot) {
                    robot.inventory.addResource(resourcesToBeDistributed.key, freeStorage)
                    freeStorage
                } else {
                    robot.inventory.addResource(resourcesToBeDistributed.key, resourcesPerRobot)
                    resourcesPerRobot
                }
            )
        }
    }

    /**
     * Distributes the leftover specified `Resource` to the `Robots`. Not all `Robots` can get a `Resource`, so the `Robots` get shuffled
     * and each gets a single `Resource` of the specified Type
     *
     * @param robotsAliveOnPlanet         the `Robots` which will get the remaining `Resources`
     * @param resourcesToBeDistributed    the amount and Type of the `Ressource` that will be distributed
     */
    private fun distributeResourcesShuffled(
        robotsAliveOnPlanet: List<Robot>,
        resourcesToBeDistributed: MutableMap.MutableEntry<ResourceType, Int>,
    ) {
        robotsAliveOnPlanet.shuffled().forEach { robot ->
            if (resourcesToBeDistributed.value > 0 && !robot.inventory.isFull()) {
                resourcesToBeDistributed.setValue(resourcesToBeDistributed.value - 1)
                robot.inventory.addResource(resourcesToBeDistributed.key, 1)
            }
        }
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
     * @return the specified `Robot`
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
     * @return the saved `Robot`
     */
    fun saveRobot(robot: Robot): Robot {
        return robotRepository.save(robot)
    }

    /**
     * Save all specified [Robots] [Robot].
     *
     * @param robots  a `List` of all Robots which have to be saved
     * @return a `List` of all saved Robots
     */
    fun saveAll(robots: List<Robot>): List<Robot> {
        return robotRepository.saveAll(robots).toList()
    }

    /**
     * Makes the specified [Robot] use an item. The function of the Item is specified via a higher order function, which
     * should be the value of the [ReparationItemType]. Those function need specify the `playerId`, `robotId` and pass the
     * [RobotRepository].
     * TODO rewrite to have a ReparationItemType as param instead of function
     *
     * @param robotId     the `UUID` of the `Robot` which should use the item.
     * @param playerId    the `UUID` of the player the `Robot` belongs to
     * @param func        a higher order function which specifies which effects an Item has. The function should be part of the [ReparationItemType]
     */
    fun useReparationItem(robotId: UUID, playerId: UUID, item: ReparationItemType) {
        val robot = this.getRobot(robotId)
        if (robot.inventory.getItemAmountByType(item) > 0) {
            item.func(playerId, robot, robotRepository)
            robot.inventory.removeItem(item)
            robotRepository.save(robot)
        } else
            throw NotEnoughItemsException("This Robot doesn't have the required Item")
    }
}

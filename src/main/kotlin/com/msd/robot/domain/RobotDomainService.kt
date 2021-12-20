package com.msd.robot.domain

import com.msd.application.GameMapService
import com.msd.application.dto.GameMapPlanetDto
import com.msd.domain.ResourceType
import com.msd.event.application.dto.RepairEventRobotDTO
import com.msd.item.domain.AttackItemType
import com.msd.item.domain.ItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.RepairItemType
import com.msd.robot.application.RestorationType
import com.msd.robot.domain.exception.NotEnoughItemsException
import com.msd.robot.domain.exception.PlanetBlockedException
import com.msd.robot.domain.exception.RobotNotFoundException
import com.msd.robot.domain.exception.TargetRobotOutOfReachException
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class RobotDomainService(
    val robotRepository: RobotRepository,
    val gameMapService: GameMapService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Check if an attack is valid, and if so, execute the attack.
     *
     * @param attacker      The robot attacking
     * @param target        The target of the attack
     * @throws TargetRobotOutOfReachException If the robots are not on the same planet
     */
    fun fight(attacker: Robot, target: Robot) {
        if (attacker.planet.planetId != target.planet.planetId)
            throw TargetRobotOutOfReachException("The attacking robot and the defending robot are not on the same planet")

        attacker.attack(target)

        saveRobot(attacker)
        saveRobot(target)
    }

    /**
     * Makes sure all resources of dead robots on the planet get distributed equally among all other robots on the
     * planet and that dead robots get deleted from the repository.
     *
     * This method should not throw any exceptions.
     * @return the list of robots whose inventory has changed
     */
    fun postFightCleanup(planetId: UUID): List<Robot> {
        val resourcesToBeDistributed = deleteDeadRobots(planetId)
        return distributeDroppedResources(planetId, resourcesToBeDistributed)
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
     *
     * @return the list of robots whose inventory has changed
     */
    private fun distributeDroppedResources(
        planetId: UUID,
        resourcesToBeDistributed: MutableMap<ResourceType, Int>
    ): List<Robot> {
        if (
            resourcesToBeDistributed[ResourceType.PLATIN] == 0 &&
            resourcesToBeDistributed[ResourceType.GOLD] == 0 &&
            resourcesToBeDistributed[ResourceType.GEM] == 0 &&
            resourcesToBeDistributed[ResourceType.IRON] == 0 &&
            resourcesToBeDistributed[ResourceType.COAL] == 0
        ) return listOf()

        logger.debug(
            "Distributing resources:\n${
            resourcesToBeDistributed.map{it.key.toString() + ": " + it.value.toString()}
            }"
        )
        val robotsAliveOnPlanet = robotRepository.findAllByPlanet_PlanetId(planetId)

        // reversed, so the most valuable resources get distributed first
        resourcesToBeDistributed.entries.sortedBy { it.key }.reversed().forEach {
            distributeDroppedResourcesOfType(it, robotsAliveOnPlanet)
        }

        robotRepository.saveAll(robotsAliveOnPlanet)
        return robotsAliveOnPlanet
    }

    /**
     * Distributes all `Resources` of a given type to all specified `Robots`.
     *
     * @param MutableMap.MutableEntry the `MapEntry` specifying how many of which `ResourceType` wil be distributed
     */
    private fun distributeDroppedResourcesOfType(
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
     * Distributes the leftover specified `Resource` to the `Robots`. Not all `Robots` can get a `Resource`, so the
     * `Robots` get shuffled and each gets a single `Resource` of the specified Type
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
     * Gets the specified [Robot].
     *
     * @param robotId the `UUID` of the robot which should be returned
     * @return the specified `Robot`
     * @throws RobotNotFoundException  if there is no `Robot` with the specified ID
     */
    fun getRobot(robotId: UUID): Robot {
        return robotRepository.findByIdOrNull(robotId)
            ?: throw RobotNotFoundException("Robot with ID $robotId not found")
    }

    /**
     * Wrapper for the repository method to find all robots on a specific planet
     *
     * @param planetId: The ID of the planet
     * @return the robots on the planet
     */
    fun getRobotsOnPlanet(planetId: UUID): List<Robot> {
        return robotRepository.findAllByPlanet_PlanetId(planetId)
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
     * is the value of the passed [RepairItemType]. If the Robot doesn't own the specified item a
     * NotEnoughItemsException is thrown.
     *
     * @param robotId     the `UUID` of the `Robot` which should use the item.
     * @param playerId    the `UUID` of the player the `Robot` belongs to.
     * @param item        the `RepairItemType` which should be used.
     * @throws NotEnoughItemsException when the specified `Robot` doesn't own the specified item.
     */
    fun useRepairItem(robotId: UUID, item: RepairItemType): List<RepairEventRobotDTO> {
        val robot = this.getRobot(robotId)
        if (robot.inventory.getItemAmountByType(item) > 0) {
            val robots = item.use(robot, robotRepository)
            robot.inventory.removeItem(item)
            robotRepository.save(robot)
            return robots
        } else
            throw NotEnoughItemsException("This Robot doesn't have the required Item", item)
    }

    /**
     * Use the specified item. A player is allowed to use the item, if the specified robot has the item in its
     * inventory.
     *
     * @throws NotEnoughItemsException when the robot does not have the specified item
     * @param userId: The UUID of the robot that's suppoed to use the item
     * @param target: Either a robot or planet UUID, depending upon the AttackItemType
     * @param item:   The kind of item to be used. The specified robot should at least have one of those in its
     *                inventory
     *
     * @return the UUID of the planet on which robots could have died.
     */
    fun useAttackItem(userId: UUID, target: UUID, item: AttackItemType): Pair<UUID, List<Robot>> {
        val user = getRobot(userId)
        if (user.inventory.getItemAmountByType(item) <= 0)
            throw NotEnoughItemsException("This Robot doesn't have the required Item", item)
        val battlefieldAndTargetRobots = item.use(user, target, robotRepository)
        user.inventory.removeItem(item)
        robotRepository.save(user)
        return battlefieldAndTargetRobots
    }

    /**
     * Makes the specified [Robot] use the specified item. The items function is specified via a higher order
     * function which is the `func`value of the itemType. If the `Robot` doesn't have enough of the specified items
     * a [NotEnoughItemsException] is thrown.
     *
     * @param robotId     the `UUID`of the `Robot` which should use the item.
     * @param itemType    the [MovementItemType] of the used item.
     * @throws NotEnoughItemsException when the `Robot` doesn't own enough of the specified `itemType`
     */
    fun useMovementItem(robotId: UUID, itemType: MovementItemType): Pair<Robot, GameMapPlanetDto> {
        val robot = this.getRobot(robotId)
        if (robot.inventory.getItemAmountByType(itemType) <= 0)
            throw NotEnoughItemsException("This Robot doesn't have the required Item", itemType)
        if (robot.planet.blocked)
            throw PlanetBlockedException("Can't move out of a blocked planet")
        val planetDTO = itemType.use(robot, robotRepository, gameMapService)
        robot.inventory.removeItem(itemType)
        robotRepository.save(robot)
        return Pair(robot, planetDTO)
    }

    /**
     * Returns all the robots belonging to the given player.
     *
     * @param playerId: The UUID of the player whose robots should be returned
     * @return a list of the robots belonging to the player
     */
    fun getRobotsByPlayer(playerId: UUID): List<Robot> {
        return robotRepository.findAllByPlayer(playerId)
    }

    /**
     * Gives a robot a new item.
     * @param robotId: The UUID of the robot which the item should be given to
     * @param itemType: The [ItemType] to give the robot
     */
    fun addItem(robotId: UUID, itemType: ItemType) {
        val robot = this.getRobot(robotId)
        robot.inventory.addItem(itemType)
        robotRepository.save(robot)
    }

    /**
     * Clear the given robots resources.
     *
     * @param robotId: The UUID of the robot whose resources should be cleared.
     * @return the taken resources
     */
    fun takeAllResources(robotId: UUID): Map<ResourceType, Int> {
        val robot = this.getRobot(robotId)
        val takenResources = robot.inventory.takeAllResources()
        robotRepository.save(robot)
        logger.debug("Emptied the resource inventory of robot $robotId")
        return takenResources
    }

    /**
     * Restore either the `energy` or `health` of a [Robot] fully. The type restored depends on the [RestorationType] passed.
     *
     * @param robotId         the id of the `Robot` that will be restored
     * @param restorationType the type that should be restored
     * @throws RobotNotFoundException  when the id doesn't match any `Robot`
     */
    fun restoreRobot(robotId: UUID, restorationType: RestorationType) {
        val robot = this.getRobot(robotId)
        when (restorationType) {
            RestorationType.HEALTH -> robot.repair()
            RestorationType.ENERGY -> robot.restoreEnergy()
        }
        robotRepository.save(robot)
        logger.debug("Restored $restorationType of robot $robotId")
    }
}

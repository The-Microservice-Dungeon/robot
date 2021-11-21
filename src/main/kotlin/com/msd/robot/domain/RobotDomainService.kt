package com.msd.robot.domain

import com.msd.application.GameMapService
import com.msd.domain.ResourceType
import com.msd.item.domain.AttackItemType
import com.msd.item.domain.ItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.ReparationItemType
import com.msd.robot.domain.exceptions.InvalidPlayerException
import com.msd.robot.domain.exceptions.NotEnoughItemsException
import com.msd.robot.domain.exceptions.OutOfReachException
import com.msd.robot.domain.exceptions.RobotNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class RobotDomainService(
    val robotRepository: RobotRepository,
    val gameMapService: GameMapService
) {

    /**
     * Check if an attack is valid, and if so, execute the attack.
     *
     * @param attacker      The robot attacking
     * @param target        The target of the attack
     * @throws OutOfReachException If the robots are not on the same planet
     */
    fun fight(attacker: Robot, target: Robot, player: UUID) {
        checkRobotBelongsToPlayer(attacker, player)

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
     * is the value of the passed [ReparationItemType]. To use an item the specified `playerId` and the `Robot's`
     * `player` must match. If the Robot doesn't own the specified item a NotEnoughItemsException
     * is thrown.
     *
     * @param robotId     the `UUID` of the `Robot` which should use the item.
     * @param playerId    the `UUID` of the player the `Robot` belongs to.
     * @param item        the `ReparationItemType` which should be used.
     * @throws NotEnoughItemsException when the specified `Robot` doesn't own the specified item.
     */
    fun useReparationItem(playerId: UUID, robotId: UUID, item: ReparationItemType) {
        val robot = this.getRobot(robotId)
        this.checkRobotBelongsToPlayer(robot, playerId)
        if (robot.inventory.getItemAmountByType(item) > 0) {
            item.func(robot, robotRepository)
            robot.inventory.removeItem(item)
            robotRepository.save(robot)
        } else
            throw NotEnoughItemsException("This Robot doesn't have the required Item", item)
    }

    /**
     * Use the specified item. A player is allowed to use the item, if the specified robot has the item in its
     * inventory and the player issuing the command owns the robot.
     *
     * @throws NotEnoughItemsException when the robot does not have the specified item
     * @param userId: The UUID of the robot that's suppoed to use the item
     * @param target: Either a robot or planet UUID, depending upon the AttackItemType
     * @param item:   The kind of item to be used. The specified robot should at least have one of those in its
     *                inventory
     *
     * @return the UUID of the planet on which robots could have died.
     */
    fun useAttackItem(userId: UUID, target: UUID, player: UUID, item: AttackItemType): UUID {
        val user = getRobot(userId)
        checkRobotBelongsToPlayer(user, player)
        if (user.inventory.getItemAmountByType(item) > 0) {
            val battlefield = item.use(user, target, robotRepository)
            user.inventory.removeItem(item)
            robotRepository.save(user)
            return battlefield
        } else
            throw NotEnoughItemsException("This Robot doesn't have the required Item", item)
    }

    /**
     * Makes the specified [Robot] use the specified item. To use an item the specified `playerId` and the `Robot's`
     * `player` must match. The items function is specified via a higher order function which is the `func`value of the
     * itemType. If the `Robot` doesn't have enough of the specified items an exception is thrown.
     *
     * @param playerId    the `UUID` of the player which owns the specified `Robot`
     * @param robotId     the `UUID`of the `Robot` which should use the item.
     * @param itemType    the [MovementItemType] of the used item.
     * @throws NotEnoughItemsException when the `Robot` doesn't own enough of the specified `itemType`
     */
    fun useMovementItem(playerId: UUID, robotId: UUID, itemType: MovementItemType) {
        val robot = this.getRobot(robotId)
        this.checkRobotBelongsToPlayer(robot, playerId)
        if (robot.inventory.getItemAmountByType(itemType) > 0) {
            itemType.func(robot, robotRepository, gameMapService)
            robot.inventory.removeItem(itemType)
            robotRepository.save(robot)
        } else
            throw NotEnoughItemsException("This Robot doesn't have the required Item", itemType)
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
        return takenResources
    }
}

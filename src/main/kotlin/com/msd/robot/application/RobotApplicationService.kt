package com.msd.robot.application

import com.msd.application.GameMapService
import com.msd.application.NoResourceOnPlanetException
import com.msd.application.dto.GameMapPlanetDto
import com.msd.command.*
import com.msd.command.application.*
import com.msd.command.application.command.*
import com.msd.core.FailureException
import com.msd.domain.ResourceType
import com.msd.event.application.EventSender
import com.msd.event.application.dto.*
import com.msd.planet.domain.Planet
import com.msd.robot.domain.LevelTooLowException
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotDomainService
import com.msd.robot.domain.UpgradeType
import com.msd.robot.domain.exception.InventoryFullException
import com.msd.robot.domain.exception.PlanetBlockedException
import com.msd.robot.domain.exception.RobotNotFoundException
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.floor

@Service
class RobotApplicationService(
    val gameMapService: GameMapService,
    val robotDomainService: RobotDomainService,
    val eventSender: EventSender,
    val successEventSender: SuccessEventSender
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Takes a list of commands and passes them on to the corresponding method.
     * All commands have to be homogeneous, meaning they can only be of a single Command-Type.
     * This method is executed asynchronous and does not block the calling controller.
     *
     * @param commands  List of commands that need to be executed.
     */

    @Async
    fun executeCommands(commands: List<Command>) {
        when (commands[0]) {
            is FightingCommand -> executeAttacks(commands as List<FightingCommand>)
                .also { logger.info("Finished executing batch of FightingCommands") }
            is FightingItemUsageCommand -> executeFightingItemUsageCommand(commands as List<FightingItemUsageCommand>)
                .also { logger.info("Finished executing batch of ItemUsageCommands") }
            is MineCommand -> executeMining(commands as List<MineCommand>)
                .also { logger.info("Finished executing batch of MineCommands") }
            is MovementItemsUsageCommand -> useMovementItem(commands as List<MovementItemsUsageCommand>)
                .also { logger.info("Finished executing batch of MovementItemUsageCommands") }
            is MovementCommand -> executeMoveCommands(commands as List<MovementCommand>)
                .also { logger.info("Finished executing batch of MovementCommands") }
            is BlockCommand -> executeBlockCommands(commands as List<BlockCommand>)
                .also { logger.info("Finished executing batch of BlockCommands") }
            is EnergyRegenCommand -> executeEnergyRegenCommands(commands as List<EnergyRegenCommand>)
                .also { logger.info("Finished executing batch of EnergyRegenCommands") }
            is RepairItemUsageCommand -> executeRepairItemUsageCommands(commands as List<RepairItemUsageCommand>)
                .also { logger.info("Finished executing batch of RepairItemUsageCommands") }
        }
    }

    /**
     * Executes the given [MovementItemsUsageCommand]. The [Robot's][Robot] `player` and the `command` `playerId` must
     * match, otherwise and exception is thrown.
     *
     * @param command   the `MovementItemsUsageCommand` specifying which `Robot` should use which `item`
     */
    private fun useMovementItem(commands: List<MovementItemsUsageCommand>) {
        val robotPlanetPairs = mutableMapOf<MovementItemsUsageCommand, Pair<Robot, GameMapPlanetDto>>()
        commands.forEach { command ->
            try {
                robotPlanetPairs[command] = robotDomainService.useMovementItem(command.robotUUID, command.itemType)
                logger.info("[${command.transactionUUID}] Successfully executed MovementItemUsageCommand")
            } catch (fe: FailureException) {
                eventSender.handleException(fe, command)
            }
        }
        successEventSender.sendMovementItemEvents(robotPlanetPairs)
    }

    /**
     * Spawns a new [Robot]. The `Robot` belongs to the specified player and will spawn on the Specified [Planet]
     *
     * @param player  the `UUID` of the player
     * @param planet the `UUID` of the `Planet`
     */
    fun spawn(player: UUID, planet: UUID): Robot {
        var robot = Robot(player, Planet(planet))
        robot = robotDomainService.saveRobot(robot)
        logger.info("Spawned robot with ID ${robot.id} on planet $planet")
        return robot
    }

    /**
     * Executes a batch of [MovementCommand]s by checking whether the robot exists and the player is the owner of the
     * robot. To get the new [Planet] the robot should be positioned on, it calls the GameMap MicroService through
     * a connector service [GameMapService]. If everything goes right, the robot gets moved and the corresponding events
     * get thrown.
     *
     * @param moveCommand a list of [MovementCommand]s containing the IDs of the robots which have to move, the players
     * who send it and the target `Planets`
     */
    fun executeMoveCommands(moveCommands: List<MovementCommand>) {
        val successfulCommands = mutableMapOf<MovementCommand, Triple<Robot, Int, GameMapPlanetDto>>()
        moveCommands.forEach { moveCommand ->
            try {
                successfulCommands[moveCommand] = move(moveCommand)
            } catch (fe: FailureException) {
                eventSender.handleException(fe, moveCommand)
            }
        }

        logger.debug("[Movement] Sending success events for command batch")
        successfulCommands.forEach { (command, triple) ->
            successEventSender.sendMovementEvents(triple.first, triple.second, command, triple.third)
        }
    }

    private fun move(
        moveCommand: MovementCommand
    ): Triple<Robot, Int, GameMapPlanetDto> {
        val robotId = moveCommand.robotUUID

        val robot = robotDomainService.getRobot(robotId)

        val planetDto =
            gameMapService.retrieveTargetPlanetIfRobotCanReach(
                robot.planet.planetId,
                moveCommand.targetPlanetUUID
            )
        val cost = planetDto.movementDifficulty
        val planet = planetDto.toPlanet()
        try {
            robot.move(planet, cost)
            logger.info("[${moveCommand.transactionUUID}] Successfully executed MoveCommand")
            return Triple(robot, cost, planetDto)
        } catch (pbe: PlanetBlockedException) {
            logger.info(
                "[${moveCommand.transactionUUID}] " +
                    "Impeded movement of robot ${robot.id} from moving because planet was blocked."
            )
            throw pbe
        } finally {
            robotDomainService.saveRobot(robot)
        }
    }

    /**
     * Makes the [Robot] specified in the [BlockCommand] block its current [Planet].
     *
     * @param blockCommand            The `BlockCommand` which specifies which robot should block
     * @throws RobotNotFoundException  if no robot with the ID specified in the `BlockCommand` can be found
     * @throws InvalidPlayerException  if the PlayerIDs specified in the `BlockCommand` and `Robot` don't match
     */
    fun executeBlockCommands(blockCommands: List<BlockCommand>) {
        blockCommands.forEach { blockCommand ->
            try {
                val robot = robotDomainService.getRobot(blockCommand.robotUUID)
                robot.block()
                robotDomainService.saveRobot(robot)
                successEventSender.sendBlockEvent(robot, blockCommand)
            } catch (fe: FailureException) {
                eventSender.handleException(fe, blockCommand)
            }
        }
    }

    /**
     * Regenerates the `energy` of a user specified in [energyRegenCommand]. If the specified [Robot] can not be found or the
     * players don't match an exception is thrown.
     *
     * @param energyRegenCommands       a list of [EnergyRegenCommand]s in which the robot which should regenerate its
     *                                  `energy` and its player is specified
     * @throws RobotNotFoundException  When a `Robot` with the specified ID can't be found
     */
    fun executeEnergyRegenCommands(energyRegenCommands: List<EnergyRegenCommand>) {
        energyRegenCommands.forEach { energyRegenCommand ->
            try {
                val robot = robotDomainService.getRobot(energyRegenCommand.robotUUID)
                robot.regenerateEnergy()
                robotDomainService.saveRobot(robot)
                successEventSender.sendEnergyRegenEvent(robot, energyRegenCommand)
            } catch (fe: FailureException) {
                eventSender.handleException(fe, energyRegenCommand)
            }
        }
    }

    /**
     * Upgrades the [Robot's][Robot] specified Upgrade to the given level.
     *
     * @param robotId     the `Robot` which should be updated
     * @param upgradeType The upgrade which should increase its level
     * @param level       the level to which the upgrade should increase
     * @throws RobotNotFoundException  if there is not `Robot` with the specified ID
     * @throws UpgradeException        if there is an attempt to skip a level, downgrade or upgrade past the max level
     */
    fun upgrade(robotId: UUID, upgradeType: UpgradeType, level: Int) {
        val robot = robotDomainService.getRobot(robotId)
        robot.upgrade(upgradeType, level)
        robotDomainService.saveRobot(robot)
        logger.info("Successfully upgraded $upgradeType of robot $robotId")
    }

    /**
     * Execute all attack commands. This has to make sure that all attacks get executed, even if a robot dies during
     * the round. After all commands have been executed, dead robots get deleted and their resources distributed
     * equally among all living robots on the planet.
     *
     * This method should never throw any exception. Exceptions occurring during the execution of a single command get
     * handled right then and should not disturb the execution of the following commands.
     *
     * @param fightingCommands    A list of AttackCommands that should be executed
     */
    fun executeAttacks(fightingCommands: List<FightingCommand>) {
        val battleFields = executeFights(fightingCommands)
        postFightCleanup(battleFields)
    }

    /**
     * Execute the fightingCommands by retrieving attacker and attacked for each command and dealing the damage.
     * For each attack a fightingEvent is emitted and the planets on which the fighting occurs are stored for
     * cleanup.
     *
     * @return the list of planet-IDs on which a fight happened.
     */
    private fun executeFights(
        fightingCommands: List<FightingCommand>
    ): MutableSet<UUID> {
        val battleFields = mutableSetOf<UUID>()
        fightingCommands.forEach {
            try {
                val attacker = robotDomainService.getRobot(it.robotUUID)
                val target = robotDomainService.getRobot(it.targetRobotUUID)

                robotDomainService.fight(attacker, target)
                successEventSender.sendFightingEvent(it, target, attacker)
                battleFields.add(attacker.planet.planetId)
            } catch (fe: FailureException) {
                eventSender.handleException(fe, it)
            }
        }
        logger.debug(
            "Successful Fights happened on following planets:\n" +
                battleFields.fold("") { agg, battlefield -> "$agg- $battlefield,\n" }
        )
        return battleFields
    }

    /**
     * Clean up the affected planets (called battleFields) and send the events for resource distribution
     */
    private fun postFightCleanup(battleFields: MutableSet<UUID>) {
        logger.debug("Starting cleanup for planets after fight")
        battleFields.forEach { planetId ->
            val affectedRobots = robotDomainService.postFightCleanup(planetId)
            logger.debug(
                "Clean up on planet $planetId affected following robots:\n" +
                    affectedRobots.fold("") { agg, robot -> "$agg- $robot.id\n" }
            )
            affectedRobots.forEach {
                successEventSender.sendResourceDistributionEvent(it)
            }
        }
    }

    /**
     * Execute the list of RepairItemUsageCommands by:
     *   - Making the specified [Robot] use the specified [ReparationItem][RepairItemType].
     *   - Handling failures by calling the eventSender without stopping the execution of the remaining commands.
     *   - Sending events after successful usage.
     *
     * @param commands a list of [RepairItemUsageCommand]s which specify which `Robot` should use which item
     */
    fun executeRepairItemUsageCommands(commands: List<RepairItemUsageCommand>) {
        commands.forEach { command ->
            try {
                val robots = robotDomainService.useRepairItem(command.robotUUID, command.itemType)
                successEventSender.sendRepairItemEvent(command, robots)
            } catch (fe: FailureException) {
                eventSender.handleException(fe, command)
            }
        }
    }

    /**
     * Executes all [AttackItemUsageCommands][FightingItemUsageCommand]. The failure of one command execution does not
     * impair the other command executions. After all commands have been executed, the battlefields get cleaned up,
     * i.e. all dead robots get removed and their resources distributed between the remaining robots on the planet.
     *
     * @param usageCommands: The AttackItemUsageCommands that should be executed
     */
    fun executeFightingItemUsageCommand(usageCommands: List<FightingItemUsageCommand>) {
        val battleFields = useFightingItem(usageCommands)
        postFightCleanup(battleFields)
    }

    /**
     * Executes the FightingItemUsageCommand
     */
    private fun useFightingItem(
        usageCommands: List<FightingItemUsageCommand>
    ): MutableSet<UUID> {
        val battleFields = mutableSetOf<UUID>()
        usageCommands.forEach {
            try {
                val robot = robotDomainService.getRobot(it.robotUUID)
                val (battlefield, targetRobots) = robotDomainService.useAttackItem(
                    it.robotUUID,
                    it.targetUUID,
                    it.itemType
                )
                successEventSender.sendAttackItemEvents(targetRobots, it, robot)
                battleFields.add(battlefield)
            } catch (fe: FailureException) {
                eventSender.handleException(fe, it)
            }
        }
        return battleFields
    }

    /**
     * Repairs the specified [Robot] to full health.
     *
     * @param robotId the [UUID] of the to be repaired robot.
     */
    fun repair(robotId: UUID) {
        val robot = robotDomainService.getRobot(robotId)
        robot.repair()
        logger.info("Successfully repair robot $robotId")
        robotDomainService.saveRobot(robot)
    }

    /**
     * Executes all mining commands.
     *
     * @param mineCommands: A list of MineCommands that need to be executed.
     */
    fun executeMining(mineCommands: List<MineCommand>) {
        val resourcesByPlanets = getResourcesOnPlanets(mineCommands)
        logger.debug("Fetched resources on planets")
        val validMineCommands = replaceIdsWithObjectsIfMineCommandIsValid(mineCommands, resourcesByPlanets)

        val amountsByGroupedPlanet = calculateResourceAmountRequestedPerPlanet(validMineCommands)

        amountsByGroupedPlanet.forEach { (planet, amount) ->
            mineResourcesOnPlanet(validMineCommands, planet, amount)
        }
        validMineCommands.forEach {
            successEventSender.sendMiningEvent(it)
        }
    }

    /**
     * Create a map of planets and the resource each offers. Each planet either has a single resource or none,
     * represented with a null value.
     *
     * @param mineCommands: A list of commands, for which the resources should be fetched.
     * @return a map assigning each planet a resource or a null value
     */
    private fun getResourcesOnPlanets(mineCommands: List<MineCommand>): Map<UUID, ResourceType?> = mineCommands
        .map {
            try {
                robotDomainService.getRobot(it.robotUUID).planet.planetId
            } catch (rnfe: RobotNotFoundException) {
                // we will handle this later, for we are just interested in the planets
                logger.debug("[Mining] Robot with ${it.robotUUID} not found")
                null
            }
        }
        .filterNotNull()
        .distinct()
        .map {
            it to try {
                gameMapService.getResourceOnPlanet(it)
            } catch (re: RuntimeException) {
                // if there was any problem we just put null, this will cause an exception to be thrown later on
                logger.debug("[Mining] Failed to get resource on planet $it from map service")
                null
            }
        }
        .filter { it.second != null }
        .toList()
        .toMap()

    /**
     * Creates a list of [ValidMineCommand]s, which represent valid [MineCommand]s but using the entity objects instead
     * of their IDs. A MineCommand is valid, if
     * 1. the specified robot UUID corresponds to an actual robot
     * 2. the planet on which the robot is positioned exists and has a resource patch on it
     * 3. the robot has the necessary mining level to mine the resource on the planet
     *
     * @param mineCommands: The list of [MineCommand]s which gets filtered for validity
     * @param planetsToResources: A map of planets assigning each a resourceType or a null value, representing no
     *                            resource present on the planet.
     * @return a list of [ValidMineCommand]s, representing only the MineCommands which are valid and having replaced
     *              the IDs with the corresponding entities.
     */
    private fun replaceIdsWithObjectsIfMineCommandIsValid(
        mineCommands: List<MineCommand>,
        planetsToResources: Map<UUID, ResourceType?>
    ): MutableList<ValidMineCommand> {
        val validMineCommands = mutableListOf<ValidMineCommand>()

        for (mineCommand in mineCommands) {
            try {
                val robot = robotDomainService.getRobot(mineCommand.robotUUID)
                val resource = planetsToResources[robot.planet.planetId]
                    ?: throw NoResourceOnPlanetException(robot.planet.planetId)
                val validMineCommand = ValidMineCommand(
                    robot, robot.planet.planetId,
                    mineCommand.transactionUUID, resource, robot.miningSpeed
                )
                if (!robot.canMine(resource))
                    throw LevelTooLowException("The mining level of the robot is too low to mine the resource $resource")
                validMineCommands.add(validMineCommand)
                logger.debug("[${mineCommand.transactionUUID}] Created ValidMineCommand")
            } catch (re: FailureException) {
                eventSender.handleException(re, mineCommand)
            }
        }
        return validMineCommands
    }

    /**
     * Return the accumulated amount of requested resources for each distinct planet in the mineCommands.
     * This is achieved by grouping the commands by their planet and then accumulating the requested amount of each
     * planet.
     *
     * @Param mineCommands: A list of ValidMineCommands, each containing a requested amount and a planet.
     * @return A map connecting the distinct planets to the amount of resources requested from their resource.
     */
    private fun calculateResourceAmountRequestedPerPlanet(mineCommands: MutableList<ValidMineCommand>): Map<UUID, Int> {
        val amountsByGroupedPlanet = mineCommands.groupingBy { it.planet }.fold(
            { _, _ -> 0 },
            { _, acc, element ->
                acc + element.amountRequested
            }
        )
        return amountsByGroupedPlanet
    }

    /**
     * Tries to mine the specified amount of resources on the planet and distributes the actual amount between the
     * mining robots on the planet.
     *
     * @param validMineCommands the list of commands specifying all minings taking place
     * @param planet: The planetId for which to execute the MineCommands
     * @param amount: pre-aggregated amount of resources to mine on the planet
     */
    private fun mineResourcesOnPlanet(
        validMineCommands: MutableList<ValidMineCommand>,
        planet: UUID,
        amount: Int
    ) {
        val miningsOnPlanet = validMineCommands.filter { it.planet == planet }
        val miningRobotsOnPlanet = miningsOnPlanet.map { it.robot }
        val resource = miningsOnPlanet[0].resource
        try {
            val minedAmount = gameMapService.mine(planet, amount)
            distributeMinedResources(miningRobotsOnPlanet, minedAmount, resource)
            logger.debug("Mined and distributed resources on planet $planet")
        } catch (re: FailureException) {
            eventSender.handleAll(re, validMineCommands.map { MineCommand(it.robot.id, it.transactionId) })
        } catch (e: RuntimeException) {
            logger.error("[Mining] Error during mining call to map service for planet $planet")
        }
    }

    /**
     * Distribute the resources to the robots.
     * There is no perfectly fair way to share those resources, but we try to get close. The current method favors
     * robots with a higher miningSpeed.
     *
     * @param robots: The robots to which the resources get distributed.
     * @param amount: The amount of resources to distribute.
     * @param resource: The type of resource of which the given amount gets distributed.
     */
    private fun distributeMinedResources(robots: List<Robot>, amount: Int, resource: ResourceType) {
        val (amountDistributed, robotsDecimalPlaces) = distributeByMiningSpeed(robots, amount, resource)

        distributeRemainingByDecimalPlaces(robotsDecimalPlaces, amount - amountDistributed, resource)

        robotDomainService.saveAll(robots)
    }

    /**
     * Distributes the mined resources to the robots corresponding to their miningSpeed.
     * A robot with the mining speed 10 gets double the amount of resources a robot with miningSpeed 5 gets.
     *
     * @return The remaining resources that could not get distributed this way, because we don't distribute partial resources
     * (fractions of a resource), together with the decimalPlaces of the fraction the robot would have
     * been assigned if we split the resources into partial resources.
     *
     * Example: Three robots with the miningSpeeds 10, 15 and 20 get distributed 22 resources. With this method
     * we can distribute 20 resources (4, 7 and 9 respectively). If we could distribute fractions of resources robot1
     * would have gotten 4.88, robot2 7.33 and robot3 9.77 resources. We distribute the whole resources and return
     * the amount left (2) together with the decimal places for each robot(0.88, 0.33 and 0.77). The decimal places can
     * be used to determine which robot(s) should get the remaining resources.
     *
     * @param robots: The robots to which to distribute the resources
     * @param amount: Amount of resources to be distributed
     * @param resource: The type of resource to be distributed
     */
    private fun distributeByMiningSpeed(
        robots: List<Robot>,
        amount: Int,
        resource: ResourceType
    ): Pair<Int, MutableMap<Robot, Double>> {
        logger.debug("Distributing $amount of type $resource by mining speed")
        val accumulatedMiningSpeed = robots.fold(0) { acc, robot -> acc + robot.miningSpeed }
        var amountDistributed = 0
        val robotsDecimalPlaces = mutableMapOf<Robot, Double>()

        robots.forEach { robot ->
            amountDistributed += distributeToRobotByMiningSpeed(
                robot,
                accumulatedMiningSpeed,
                amount,
                robotsDecimalPlaces,
                resource
            )
        }
        return Pair(amountDistributed, robotsDecimalPlaces)
    }

    /**
     * Distribute the resource to the robot by its fair share, calculated by the portion of mining speed it has
     * relative to the mining speed of all robots mining on the same planet.
     *
     * @param robot: The robot to receive the resources
     * @param accumulatedMiningSpeed: The mining speed of all robots mining on the planet combined
     * @param amount: The overall amount to be distributed to all robots
     * @param robotsDecimalPlaces: Map that needs to get updated with the decimal places of the correspondingAmount
     * @param resourceType: The ResourceType of the distributed resource
     *
     * @return the amount distributed to the robot
     */
    private fun distributeToRobotByMiningSpeed(
        robot: Robot,
        accumulatedMiningSpeed: Int,
        amount: Int,
        robotsDecimalPlaces: MutableMap<Robot, Double>,
        resourceType: ResourceType
    ): Int {
        val correspondingAmount = floor((robot.miningSpeed.toDouble() / accumulatedMiningSpeed) * amount).toInt()
        val remainder = ((robot.miningSpeed.toDouble() / accumulatedMiningSpeed) * amount) - correspondingAmount
        robotsDecimalPlaces[robot] = remainder
        try {
            robot.inventory.addResource(resourceType, correspondingAmount)
        } catch (ife: InventoryFullException) {
            logger.info("[Mining] Robot did not receive all resources granted to it, because its inventory was full")
        }
        return correspondingAmount
    }

    /**
     * Distributes the remainingAmount of resources to the robots, favoring the robots with the highest decimalPlaces.
     *
     * @param robotsDecimalPlaces: A map assigning each robot the remaining fraction of a resource it didn't get
     *                             but deserved due to it's mining level.
     * @param remainingAmount: The remaining amount of resources to be distributed
     * @param resource: The type of resource to be distributed
     *
     * Example: The robots get the remaining resources one by one starting with the robot with the highest remaining
     * fraction. The fractions (0.88, 0.33 and 0.77) with 2 resources to be distributed lead to a distribution of
     *  robot1: 1
     *  robot2: 0
     *  robot3: 1
     */
    private fun distributeRemainingByDecimalPlaces(
        robotsDecimalPlaces: MutableMap<Robot, Double>,
        remainingAmount: Int,
        resource: ResourceType
    ) {
        logger.debug("Distributing $remainingAmount of type $resource by decimal places")
        var amountDistributed = 0
        val sortedDecimalPlaces = robotsDecimalPlaces.entries.sortedBy { it.value }.reversed()
        var index = 0
        val fullRobots = mutableMapOf<Robot, Boolean>()
        while (amountDistributed < remainingAmount && fullRobots.count() < sortedDecimalPlaces.size) {
            try {
                sortedDecimalPlaces[index % sortedDecimalPlaces.size].key.inventory.addResource(resource, 1)
                amountDistributed += 1
            } catch (ife: InventoryFullException) {
                fullRobots[sortedDecimalPlaces[index % sortedDecimalPlaces.size].key] = true
                logger.debug(
                    "[Mining] Robot did not receive all resources granted to it, because its " +
                        "inventory was full"
                )
            }
            index++
        }
    }
}

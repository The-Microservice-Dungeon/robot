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
import com.msd.event.application.EventType
import com.msd.event.application.dto.*
import com.msd.planet.application.PlanetMapper
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetType
import com.msd.robot.domain.LevelTooLowException
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotDomainService
import com.msd.robot.domain.UpgradeType
import com.msd.robot.domain.exception.InventoryFullException
import com.msd.robot.domain.exception.PlanetBlockedException
import com.msd.robot.domain.exception.RobotNotFoundException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.floor

@Service
class RobotApplicationService(
    val gameMapService: GameMapService,
    val robotDomainService: RobotDomainService,
    val eventSender: EventSender,
    val planetMapper: PlanetMapper
) {

    /**
     * Takes a list of commands and passes them on to the corresponding method.
     * [FightingCommand]s and [FightingItemUsageCommand]s are homogeneous and have to be handled as one single batch.
     * All other commands can be heterogeneous and thus can be passed to the corresponding methods individually.
     * This method is executed asynchronous and does not block the calling controller.
     *
     * @param commands  List of commands that need to be executed.
     */

    @Async
    fun executeCommands(commands: List<Command>) {
        if (commands[0] is FightingCommand)
        // Attack commands are always homogenous, so this cast is valid
            executeAttacks(commands as List<FightingCommand>)
        else if (commands[0] is FightingItemUsageCommand)
            useAttackItems(commands as List<FightingItemUsageCommand>)
        else if (commands[0] is MineCommand)
            executeMining(commands as List<MineCommand>)
        else
            executeHeterogeneousCommands(commands)
    }

    /**
     * Execute every command in the list and pass occuring exceptions on to the handler.
     *
     * @param commands  The commands that need to be executed. These can be heterogeneous.
     */
    private fun executeHeterogeneousCommands(commands: List<Command>) {
        commands.forEach {
            try {
                when (it) {
                    is MovementCommand -> move(it)
                    is BlockCommand -> block(it)
                    is EnergyRegenCommand -> regenerateEnergy(it)
                    is RepairItemUsageCommand -> useRepairItem(it)
                    is MovementItemsUsageCommand -> useMovementItem(it)
                }
            } catch (fe: FailureException) {
                eventSender.handleException(fe, it)
            }
        }
    }

    /**
     * Executes the given [MovementItemsUsageCommand]. The [Robot's][Robot] `player` and the `command` `playerId` must
     * match, otherwise and exception is thrown.
     *
     * @param command   the `MovementItemsUsageCommand` specifying which `Robot` should use which `item`
     */
    private fun useMovementItem(command: MovementItemsUsageCommand) {
        val (robot, planetDTO) = robotDomainService.useMovementItem(command.robotUUID, command.itemType)
        val moveEventId = sendMovementEvent(robot, planetDTO.movementDifficulty, command.transactionUUID)
        eventSender.sendEvent(
            ItemMovementEventDTO(
                true,
                "Item usage successful",
                moveEventId
            ),
            EventType.ITEM_MOVEMENT,
            command.transactionUUID
        )
    }

    /**
     * Spawns a new [Robot]. The `Robot` belongs to the specified player and will spawn on the Specified [Planet]
     *
     * @param player  the `UUID` of the player
     * @param planet the `UUID` of the `Planet`
     */
    fun spawn(player: UUID, planet: UUID): Robot {
        val robot = Robot(player, Planet(planet))
        return robotDomainService.saveRobot(robot)
    }

    /**
     * Executes a single [MovementCommand] by checking whether the robot exists and the player is the owner of the
     * robot. To get the new [Planet] the robot should be positioned on, if calls the GameMap MicroService through
     * a connector service [GameMapService]. If everything goes right, the robot gets moved.
     *
     * @param moveCommand a [Command] containing the IDs of the Robot which has to move, the Player who send it and the target `Planet`
     */
    fun move(moveCommand: MovementCommand) {
        val robotId = moveCommand.robotUUID

        val robot = robotDomainService.getRobot(robotId)

        val planetDto =
            gameMapService.retrieveTargetPlanetIfRobotCanReach(robot.planet.planetId, moveCommand.targetPlanetUUID)
        val cost = planetDto.movementDifficulty
        val planet = planetDto.toPlanet()
        try {
            robot.move(planet, cost)
            robotDomainService.saveRobot(robot)
            sendMovementEvents(robot, cost, moveCommand, planetDto)
        } catch (pbe: PlanetBlockedException) {
            robotDomainService.saveRobot(robot)
            throw pbe
        }
    }

    private fun sendMovementEvent(
        robot: Robot,
        cost: Int,
        transactionUUID: UUID
    ): UUID {
        return eventSender.sendEvent(
            MovementEventDTO(
                true,
                "Movement successful",
                robot.energy,
                planetMapper.planetToPlanetDTO(robot.planet, cost, PlanetType.DEFAULT), // TODO planet type?
                robotDomainService.getRobotsOnPlanet(robot.planet.planetId).map { it.id }
            ),
            EventType.MOVEMENT,
            transactionUUID
        )
    }

    /**
     * Sends the events due after a successful movement command execution
     *
     * @param robot: The robot that moved
     * @param cost: The energy costs of the movement
     * @param moveCommand: The move command that was executed
     * @param planetDto: The planet to which the robot moved, as returned from the Map Service
     */
    private fun sendMovementEvents(
        robot: Robot,
        cost: Int,
        moveCommand: MovementCommand,
        planetDto: GameMapPlanetDto
    ) {
        eventSender.sendEvent(
            MovementEventDTO(
                true,
                "Movement successful",
                robot.energy,
                planetMapper.planetToPlanetDTO(robot.planet, cost, PlanetType.DEFAULT), // TODO planet type?
                robotDomainService.getRobotsOnPlanet(robot.planet.planetId).map { it.id }
            ),
            EventType.MOVEMENT,
            moveCommand.transactionUUID
        )
        eventSender.sendEvent(
            NeighboursEventDTO(
                planetDto.neighbours.map {
                    NeighboursEventDTO.NeighbourDTO(it.planetId, it.movementDifficulty, it.direction)
                }
            ),
            EventType.NEIGHBOURS,
            moveCommand.transactionUUID
        )
    }

    /**
     * Makes the [Robot] specified in the [BlockCommand] block its current [Planet].
     *
     * @param blockCommand            The `BlockCommand` which specifies which robot should block
     * @throws RobotNotFoundException  if no robot with the ID specified in the `BlockCommand` can be found
     * @throws InvalidPlayerException  if the PlayerIDs specified in the `BlockCommand` and `Robot` don't match
     */
    fun block(blockCommand: BlockCommand) {
        val robot = robotDomainService.getRobot(blockCommand.robotUUID)
        robot.block()
        robotDomainService.saveRobot(robot)
        eventSender.sendEvent(
            BlockEventDTO(
                true,
                "Planet with ID: ${robot.planet.planetId} has been blocked",
                robot.planet.planetId,
                robot.energy
            ),
            EventType.PLANET_BLOCKED,
            blockCommand.transactionUUID
        )
    }

    /**
     * Regenerates the `energy` of a user specified in [energyRegenCommand]. If the specified [Robot] can not be found or the
     * players don't match an exception is thrown.
     *
     * @param energyRegenCommand       a [EnergyRegenCommand] in which the robot which should regenerate its `energy` and its Player is specified
     * @throws RobotNotFoundException  When a `Robot` with the specified ID can't be found
     * @throws InvalidPlayerException  When the specified `Player` and the `Player` specified in the `Robot` don't match
     */
    fun regenerateEnergy(energyRegenCommand: EnergyRegenCommand) {
        val robot = robotDomainService.getRobot(energyRegenCommand.robotUUID)
        robot.regenerateEnergy()
        robotDomainService.saveRobot(robot)
        eventSender.sendEvent(
            RegenerationEventDTO(
                true,
                "Robot regenerated ${robot.energyRegen} energy",
                robot.energy
            ),
            EventType.REGENERATION,
            energyRegenCommand.transactionUUID
        )
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
    }

    /**
     * Execute all attack commands. This has to make sure that all attacks get executed, even if a robot dies during
     * the round. After all commands have been executed, dead robots get deleted and their resources distributed
     * equally among all living robots on the planet.
     *
     * This method should never throw any exception. Exceptions occurring during the execution of a single command get
     * handled right then and should not disturb the execution of the following commands.
     *
     * @param fightingCommands    A list of AttackCommands that need to be executed
     */
    fun executeAttacks(fightingCommands: List<FightingCommand>) {
        val battleFields = mutableSetOf<UUID>()
        executeFights(fightingCommands, battleFields)
        postFightCleanup(battleFields)
    }

    private fun executeFights(
        fightingCommands: List<FightingCommand>,
        battleFields: MutableSet<UUID>
    ) {
        fightingCommands.forEach {
            try {
                val attacker = robotDomainService.getRobot(it.robotUUID)
                val target = robotDomainService.getRobot(it.targetRobotUUID)

                robotDomainService.fight(attacker, target)
                eventSender.sendEvent(
                    FightingEventDTO(
                        true,
                        "Attacking successful",
                        it.robotUUID,
                        it.targetRobotUUID,
                        target.health,
                        attacker.energy
                    ),
                    EventType.FIGHTING,
                    it.transactionUUID
                )
                battleFields.add(attacker.planet.planetId)
            } catch (fe: FailureException) {
                eventSender.handleException(fe, it)
            }
        }
    }

    private fun postFightCleanup(battleFields: MutableSet<UUID>) {
        battleFields.forEach { planetId ->
            val affectedRobots = robotDomainService.postFightCleanup(planetId)
            affectedRobots.forEach {
                eventSender.sendGenericEvent(
                    ResourceDistributionEventDTO(
                        it.id,
                        it.inventory.getStorageUsageForResource(ResourceType.COAL),
                        it.inventory.getStorageUsageForResource(ResourceType.IRON),
                        it.inventory.getStorageUsageForResource(ResourceType.GEM),
                        it.inventory.getStorageUsageForResource(ResourceType.GOLD),
                        it.inventory.getStorageUsageForResource(ResourceType.PLATIN),
                    ),
                    EventType.RESOURCE_DISTRIBUTION
                )
            }
        }
    }

    /**
     * Makes the specified [Robot] use the specified [ReparationItem][RepairItemType].
     *
     * @param command the [RepairItemUsageCommand] which specifies which `Robot` should use which item
     */
    fun useRepairItem(command: RepairItemUsageCommand) {
        val robots = robotDomainService.useRepairItem(command.robotUUID, command.itemType)
        eventSender.sendEvent(
            ItemRepairEventDTO(
                true,
                "Robot has used ${command.itemType}",
                robots
            ),
            EventType.ITEM_REPAIR,
            command.transactionUUID
        )
    }

    /**
     * Execute all [AttackItemUsageCommands][FightingItemUsageCommand]. The failure of one command execution does not
     * impair the other command executions. After all commands have been executed, the battlefields get cleaned up,
     * i.e. all dead robots get removed and their resources distributed between the remaining robots on the planet.
     *
     * @param usageCommands: The AttackItemUsageCommands that should be executed
     */
    fun useAttackItems(usageCommands: List<FightingItemUsageCommand>) {
        val battleFields = mutableSetOf<UUID>()
        usageCommands.forEach {
            try {
                val robot = robotDomainService.getRobot(it.robotUUID)
                val (battlefield, targetRobots) = robotDomainService.useAttackItem(
                    it.robotUUID,
                    it.targetUUID,
                    it.itemType
                )
                val causedFightingEvents = targetRobots.map { targetRobot ->
                    eventSender.sendEvent(
                        FightingEventDTO(
                            true,
                            "Attacking successful",
                            it.robotUUID,
                            targetRobot.id,
                            targetRobot.health,
                            robot.energy
                        ),
                        EventType.FIGHTING,
                        it.transactionUUID
                    )
                }
                eventSender.sendEvent(
                    ItemFightingEventDTO(
                        true,
                        "Item usage successful",
                        robot.inventory.getItemAmountByType(it.itemType),
                        causedFightingEvents
                    ),
                    EventType.ITEM_FIGHTING,
                    it.transactionUUID
                )
                battleFields.add(battlefield)
            } catch (fe: FailureException) {
                eventSender.handleException(fe, it)
            }
        }
        postFightCleanup(battleFields)
    }

    /**
     * Repairs the specified [Robot] to full health.
     *
     * @param robotId the [UUID] of the to be repaired robot.
     */
    fun repair(robotId: UUID) {
        val robot = robotDomainService.getRobot(robotId)

        robot.repair()
        robotDomainService.saveRobot(robot)
    }

    /**
     * Executes all mining commands.
     *
     * @param mineCommands: A list of MineCommands that need to be executed.
     */
    fun executeMining(mineCommands: List<MineCommand>) {
        val resourcesByPlanets = getResourcesOnPlanets(mineCommands)

        val validMineCommands = replaceIdsWithObjectsInValidMineCommands(mineCommands, resourcesByPlanets)

        val amountsByGroupedPlanet = resourceAmountRequestedPerPlanet(validMineCommands)

        amountsByGroupedPlanet.forEach { (planet, amount) ->
            mineResourcesOnPlanet(validMineCommands, planet, amount)
        }

        validMineCommands.forEach {
            eventSender.sendEvent(
                MiningEventDTO(
                    true,
                    "Robot ${it.robot.id} mined successfully",
                    it.robot.energy,
                    it.robot.inventory.getStorageUsageForResource(it.resource),
                    it.resource.toString()
                ),
                EventType.MINING,
                it.transactionId
            )
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
    private fun replaceIdsWithObjectsInValidMineCommands(
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
    private fun resourceAmountRequestedPerPlanet(mineCommands: MutableList<ValidMineCommand>): Map<UUID, Int> {
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
        } catch (re: FailureException) {
            eventSender.handleAll(re, validMineCommands.map { MineCommand(it.robot.id, it.transactionId) })
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
        val accumulatedMiningSpeed = robots.fold(0) { acc, robot -> acc + robot.miningSpeed }
        var amountDistributed = 0
        val robotsDecimalPlaces = mutableMapOf<Robot, Double>()

        robots.forEach {
            val correspondingAmount = floor((it.miningSpeed.toDouble() / accumulatedMiningSpeed) * amount).toInt()
            val remainder = ((it.miningSpeed.toDouble() / accumulatedMiningSpeed) * amount) - correspondingAmount
            amountDistributed += correspondingAmount
            robotsDecimalPlaces[it] = remainder
            try {
                it.inventory.addResource(resource, correspondingAmount)
            } catch (ife: InventoryFullException) {
                // TODO?
            }
        }
        return Pair(amountDistributed, robotsDecimalPlaces)
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
            }
            index++
        }
    }
}

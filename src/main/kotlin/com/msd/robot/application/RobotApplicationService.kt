package com.msd.robot.application

import com.msd.application.ExceptionConverter
import com.msd.application.GameMapService
import com.msd.command.application.*
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotDomainService
import com.msd.robot.domain.UpgradeType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*

@Service
class RobotApplicationService(
    val gameMapService: GameMapService,
    val robotDomainService: RobotDomainService,
    val exceptionConverter: ExceptionConverter
) {

    /**
     * Takes a list of commands and passes them on to the corresponding method.
     * [AttackCommand]s and [AttackItemUsageCommand]s are homogeneous and have to be handled as one single batch.
     * All other commands can be heterogeneous and thus can be passed to the corresponding methods individually.
     * This method is executed asynchronous and does not block the calling controller.
     *
     * @param commands  List of commands that need to be executed.
     */
    @Async
    fun executeCommands(commands: List<Command>) {
        if (commands[0] is AttackCommand)
        // Attack commands are always homogenous, so this cast is valid
            executeAttacks(commands as List<AttackCommand>)
        else if (commands[0] is AttackItemUsageCommand)
            useAttackItems(commands as List<AttackItemUsageCommand>)
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
                    // TODO Add remaining CommandTypes as soon as their methods are implemented
                }
            } catch (re: RuntimeException) {
                exceptionConverter.handle(re, it.transactionUUID)
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
        robotDomainService.useMovementItem(command.robotUUID, command.itemType)
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
        val cost = planetDto.movementCost
        val planet = planetDto.toPlanet()
        robot.move(planet, cost)
        robotDomainService.saveRobot(robot)
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
     * @param attackCommands    A list of AttackCommands that need to be executed
     */
    fun executeAttacks(attackCommands: List<AttackCommand>) {
        val battleFields = mutableSetOf<UUID>()
        attackCommands.forEach {
            try {
                val attacker = robotDomainService.getRobot(it.robotUUID)
                val target = robotDomainService.getRobot(it.targetRobotUUID)

                robotDomainService.fight(attacker, target)
                battleFields.add(attacker.planet.planetId)
            } catch (re: RuntimeException) {
                exceptionConverter.handle(re, it.transactionUUID)
            }
        }

        battleFields.forEach { planetId ->
            robotDomainService.postFightCleanup(planetId)
        }
    }

    /**
     * Makes the specified [Robot] use the specified [ReparationItem][RepairItemType].
     *
     * @param command the [RepairItemUsageCommand] which specifies which `Robot` should use which item
     */
    fun useRepairItem(command: RepairItemUsageCommand) {
        robotDomainService.useRepairItem(command.robotUUID, command.itemType)
    }

    /**
     * Execute all [AttackItemUsageCommands][AttackItemUsageCommand]. The failure of one command execution does not
     * impair the other command executions. After all commands have been executed, the battlefields get cleaned up,
     * i.e. all dead robots get removed and their resources distributed between the remaining robots on the planet.
     *
     * @param usageCommands: The AttackItemUsageCommands that should be executed
     */
    fun useAttackItems(usageCommands: List<AttackItemUsageCommand>) {
        val battleFields = mutableSetOf<UUID>()
        usageCommands.forEach {
            try {
                val battlefield = robotDomainService.useAttackItem(it.robotUUID, it.targetUUID, it.itemType)
                battleFields.add(battlefield)
            } catch (re: RuntimeException) {
                exceptionConverter.handle(re, it.transactionUUID)
            }
        }

        battleFields.forEach { planetId ->
            robotDomainService.postFightCleanup(planetId)
        }
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
}

package com.msd.robot.application

import com.msd.application.ExceptionConverter
import com.msd.application.GameMapService
import com.msd.command.application.*
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotDomainService
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
            TODO() // this needs to be handled in a batch as well
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
                    is ReparationItemUsageCommand -> useReparationItem(it)
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
        robotDomainService.useMovementItem(command.playerUUID, command.robotUUID, command.itemType)
    }

    /**
     * Spawns a new [Robot]. The `Robot` belongs to the specified player and will spawn on the Specified [Planet]
     *
     * @param player  the `UUID` of the player
     * @param planet the `UUID` of the `Planet`
     */
    fun spawn(player: UUID, planet: UUID) {
        val robot = Robot(player, Planet(planet))
        robotDomainService.saveRobot(robot)
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
        val playerId = moveCommand.playerUUID

        val robot = robotDomainService.getRobot(robotId)

        robotDomainService.checkRobotBelongsToPlayer(robot, playerId)
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
        robotDomainService.checkRobotBelongsToPlayer(robot, blockCommand.playerUUID)
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

        robotDomainService.checkRobotBelongsToPlayer(robot, energyRegenCommand.playerUUID)
        robot.regenerateEnergy()
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
                robotDomainService.checkRobotBelongsToPlayer(attacker, it.playerUUID)

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
     * Makes the specified [Robot] use the specified [ReparationItem][ReparationItemType].
     *
     * @param command the [ReparationItemUsageCommand] which specifies which `Robot` should use which item
     */
    fun useReparationItem(command: ReparationItemUsageCommand) {
        robotDomainService.useReparationItem(command.playerUUID, command.robotUUID, command.itemType)
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

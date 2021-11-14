package com.msd.robot.application

import com.msd.application.CustomExceptionHandler
import com.msd.application.GameMapService
import com.msd.command.domain.*
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotDomainService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*

@Service
class RobotApplicationService(
    val gameMapService: GameMapService,
    val robotDomainService: RobotDomainService,
    val exceptionHandler: CustomExceptionHandler
) {

    @Async
    fun executeCommandsAsynchronous(commands: List<Command>) {
        TODO()
        // Execute commands and send failure / success events with kafka
    }

    /**
     * Executes a single [MovementCommand] by checking whether the robot exists and the player is the owner of the
     * robot. To get the new [Planet] the robot should be positioned on, if calls the GameMap MicroService through
     * a connector service [GameMapService]. If everything goes right, the robot gets moved.
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
     * @param energyRegenCommand             a [EnergyRegenCommand] in which the robot which should regenerate its `energy` and its Player is specified
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
     * This method should never throw any exception. Exceptions occuring during the execution of a single command get
     * handled right then and should not disturb the execution of the following commands.
     *
     * @param attackCommands        A list of AttackCommands that need to be executed
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
                exceptionHandler.handle(re, it.transactionUUID)
            }
        }

        battleFields.forEach { planetId ->
            robotDomainService.postFightCleanup(planetId)
        }
    }

    /**
     * Repairs the specified [Robot] to full health.
     *
     * @param robotId             the [UUID] of the to be repaired robot.
     */
    fun repair(robotId: UUID) {
        val robot = robotDomainService.getRobot(robotId)

        robot.repair()
        robotDomainService.saveRobot(robot)
    }
}

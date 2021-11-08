package com.msd.robot.application

import com.msd.application.CustomExceptionHandler
import com.msd.application.GameMapService
import com.msd.command.AttackCommand
import com.msd.command.BlockCommand
import com.msd.command.MovementCommand
import com.msd.command.RegenCommand
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotDomainService
import org.springframework.stereotype.Service
import java.util.*

@Service
class RobotApplicationService(
    val gameMapService: GameMapService,
    val robotDomainService: RobotDomainService,
    val exceptionHandler: CustomExceptionHandler
) {

    /**
     * Executes a single [MovementCommand] by checking whether the robot exists and the player is the owner of the
     * robot. To get the new [Planet] the robot should be positioned on, if calls the GameMap MicroService through
     * a connector service [GameMapService]. If everything goes right, the robot gets moved.
     */
    fun move(moveCommand: MovementCommand) {
        val robotId = moveCommand.robotId
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
        val robot = robotDomainService.getRobot(blockCommand.robotId)
        robotDomainService.checkRobotBelongsToPlayer(robot, blockCommand.playerUUID)
        robot.block()
        robotDomainService.saveRobot(robot)
    }

    /**
     * Regenerates the `energy` of a user specified in [regenCommand]. If the specified [Robot] can not be found or the
     * players don't match an exception is thrown.
     *
     * @param regenCommand             a [RegenCommand] in which the robot which should regenerate its `energy` and its Player is specified
     * @throws RobotNotFoundException  When a `Robot` with the specified ID can't be found
     * @throws InvalidPlayerException  When the specified `Player` and the `Player` specified in the `Robot` don't match
     */
    fun regenerateEnergy(regenCommand: RegenCommand) {
        val robot = robotDomainService.getRobot(regenCommand.robotId)

        robotDomainService.checkRobotBelongsToPlayer(robot, regenCommand.playerId)
        robot.regenerateEnergy()
        robotDomainService.saveRobot(robot)
    }

    fun executeAttacks(attackCommands: List<AttackCommand>) {
        val battleFields = mutableSetOf<UUID>()
        attackCommands.forEach {
            try {
                val attacker = robotDomainService.getRobot(it.robotId)
                val target = robotDomainService.getRobot(it.targetRobotUUID)
                robotDomainService.checkRobotBelongsToPlayer(attacker, it.playerUUID)

                robotDomainService.fight(attacker, target)
                battleFields.add(attacker.planet.planetId)
            } catch (re: RuntimeException) {
                re.printStackTrace()
                exceptionHandler.handle(re, it.transactionUUID)
            }
        }

        battleFields.forEach { planetId ->
            robotDomainService.postFightCleanup(planetId)
        }
    }
}

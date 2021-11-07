package com.msd.robot.domain

import com.msd.robot.application.InvalidPlayerException
import com.msd.robot.application.RobotNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class RobotDomainService(
    val robotRepository: RobotRepository
) {

    fun attackRobot(attackerId: UUID, targetId: UUID) {
    }

    /**
     * Checks if the specified `playerId` matches with the [Robot's][Robot].
     *
     * @param robot      the `Robot` whose ownership will be checked
     * @param playerId   the `playerUUID` which should be checked against the `Robot`
     * @return `true` if the IDs match
     * @throws InvalidPlayerException if the Robot's ID doesn't match the specified ID
     */
    fun doesRobotBelongsToPlayer(robot: Robot, playerId: UUID) {
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
}

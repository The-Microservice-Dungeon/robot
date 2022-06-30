package com.msd.item.domain

import com.msd.domain.InvalidTargetException
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.exception.RobotNotFoundException
import com.msd.robot.domain.exception.TargetRobotOutOfReachException
import org.springframework.data.repository.findByIdOrNull
import java.util.*
/*
enum class AttackItemType(val use: (Robot, UUID, RobotRepository) -> Pair<UUID, List<Robot>>) : ItemType {
    ROCKET(::useRocket),
    LONG_RANGE_BOMBARDMENT(::useBombardment),
    SELF_DESTRUCTION(::useSelfDestruct),
    NUKE(::useNuke)
}

/**
 * Use a rocket item. A rocket deals damage to a specific robot on the same planet.
 *
 * @param user: The robot using the rocket item
 * @param target: The UUID of a robot on the same planet
 * @param robotRepo: Repository for retrieving affected robots
 *
 * @throws RobotNotFoundException when the specified target UUID does not belong to any robot
 * @throws TargetRobotOutOfReachException when the target robot is not on the same planet as the user
 *
 * @return the UUID of the planet on which the fight happened
 */
private fun useRocket(user: Robot, target: UUID, robotRepo: RobotRepository): Pair<UUID, List<Robot>> {
    val targetRobot = robotRepo.findByIdOrNull(target)
        ?: throw RobotNotFoundException("The rocket couldn't find a robot with that UUID")
    if (targetRobot.planet.planetId != user.planet.planetId)
        throw TargetRobotOutOfReachException("The target robot is not on the same planet as the using robot")
    targetRobot.receiveDamage(5)
    robotRepo.save(targetRobot)
    return targetRobot.planet.planetId to listOf(targetRobot)
}

/**
 * Use a bombardment item. A bombardment deals damage to all robots on a planet. This planet does not need to be the
 * same as the planet on which the user is located, but is allowed to be.
 *
 * @param user: The robot using the bombardment item
 * @param target: The UUID of the target planet
 * @param robotRepo: Repository for retrieving affected robots
 *
 * @return the UUID of the planet on which the fight happened
 */
private fun useBombardment(user: Robot, target: UUID, robotRepo: RobotRepository): Pair<UUID, List<Robot>> {
    val targetRobots = robotRepo.findAllByPlanet_PlanetId(target)
    targetRobots.forEach {
        it.receiveDamage(10)
    }
    robotRepo.saveAll(targetRobots)
    return target to targetRobots
}

/**
 * Use a self destruction item. Self destruction deals damage to all robots on the same planet as the planet of the
 * user. The using robot gets destroyed by using this item.
 *
 * @param user: The robot using the self destruct item
 * @param target: Also the robot using the item
 * @param robotRepo: Repository for retrieving affected robots
 *
 * @return the UUID of the planet on which the fight happened
 */
private fun useSelfDestruct(user: Robot, target: UUID, robotRepo: RobotRepository): Pair<UUID, List<Robot>> {
    if (user.id != target) throw InvalidTargetException("Robot cannot self-destruct other robot than itself")
    user.receiveDamage(1000)
    val targetRobots = robotRepo.findAllByPlanet_PlanetId(user.planet.planetId)
    targetRobots.forEach {
        it.receiveDamage(20)
    }
    robotRepo.saveAll(targetRobots)
    return user.planet.planetId to targetRobots
}

/**
 * Use a nuke item. A nuke deals damage to all robots on the specified planet.
 *
 * @param user: The robot using the nuke
 * @param target: The UUID of the target planet
 * @param robotRepo: Repository for retrieving affected robots
 *
 * @return the UUID of the planet on which the fight happened
 */
private fun useNuke(user: Robot, target: UUID, robotRepo: RobotRepository): Pair<UUID, List<Robot>> {
    val targetRobots = robotRepo.findAllByPlanet_PlanetId(target)
    targetRobots.forEach {
        it.receiveDamage(100)
    }
    robotRepo.saveAll(targetRobots)
    return target to targetRobots
}
*/
package com.msd.item.domain

import com.msd.robot.application.RobotNotFoundException
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import org.springframework.data.repository.findByIdOrNull
import java.util.*

enum class AttackItemType(val use: (Robot, UUID, RobotRepository) -> UUID) : ItemType {
    ROCKET(::useRocket),
    BOMBARDMENT(::useBombardment),
    SELF_DESTRUCT(::useSelfDestruct),
    NUKE(::useNuke)
}

private fun useRocket(user: Robot, target: UUID, robotRepo: RobotRepository): UUID {
    val targetRobot = robotRepo.findByIdOrNull(target)
        ?: throw RobotNotFoundException("The rocket couldn't find a robot with that UUID")
    targetRobot.receiveDamage(5)
    robotRepo.save(targetRobot)
    return targetRobot.planet.planetId
}

private fun useBombardment(user: Robot, target: UUID, robotRepo: RobotRepository): UUID {
    val targetRobots = robotRepo.findAllByPlanet_PlanetId(target)
    targetRobots.forEach {
        it.receiveDamage(10)
    }
    robotRepo.saveAll(targetRobots)
    return target
}

private fun useSelfDestruct(user: Robot, target: UUID, robotRepo: RobotRepository): UUID {
    user.receiveDamage(1000)
    val targetRobots = robotRepo.findAllByPlanet_PlanetId(user.planet.planetId)
    targetRobots.forEach {
        it.receiveDamage(20)
    }
    robotRepo.saveAll(targetRobots)
    return user.planet.planetId
}

private fun useNuke(user: Robot, target: UUID, robotRepo: RobotRepository): UUID {
    val targetRobots = robotRepo.findAllByPlanet_PlanetId(target)
    targetRobots.forEach {
        it.receiveDamage(100)
    }
    robotRepo.saveAll(targetRobots)
    return target
}

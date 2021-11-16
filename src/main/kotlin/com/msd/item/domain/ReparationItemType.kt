package com.msd.item.domain

import com.msd.robot.application.RobotNotFoundException
import com.msd.robot.domain.RobotRepository
import org.springframework.data.repository.findByIdOrNull
import java.util.*

enum class ReparationItemType(val func: (UUID, UUID, RobotRepository) -> Unit) {
    REPARATION_SWARM(::useRepairSwarm)
}

fun useRepairSwarm(playerId: UUID, robotId: UUID, robotRepository: RobotRepository) {
    val robot = robotRepository.findByIdOrNull(robotId)
        ?: throw RobotNotFoundException("There is no robot with the ID $robotId")
    val robots = robotRepository.findAllByPlayerAndPlanet_PlanetId(playerId, robot.planet.planetId)
    robots.forEach {
        it.repairBy(20)
    }
    robotRepository.saveAll(robots)
}

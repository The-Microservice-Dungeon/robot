package com.msd.item.domain

import com.msd.event.application.dto.RepairEventRobotDTO
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import java.util.*

enum class RepairItemType(val use: (Robot, RobotRepository) -> List<RepairEventRobotDTO>) : ItemType {
    REPAIR_SWARM(::useRepairSwarm)
}

/**
 * Repairs all [Robots][Robot] of the specified player on the [Planet] the specified Robot is on. All `Robots` heal
 * 20 health. Health can't be healed past `maxHealth`.
 *
 * @param playerId          the `UUID` of the player which owns the specified `Robot`
 * @param robot             the `Robot` which should use the item
 * @param robotRepository   the [RobotRepository] containing all Robots. Necessary so all Robots can be correctly saved
 */
private fun useRepairSwarm(robot: Robot, robotRepository: RobotRepository): List<RepairEventRobotDTO> {
    val robots = robotRepository.findAllByPlayerAndPlanet_PlanetId(robot.player, robot.planet.planetId)
    robots.forEach {
        it.repairBy(20)
    }
    robotRepository.saveAll(robots)
    return robots.map { RepairEventRobotDTO(it.id, it.health) }
}

package com.msd.item.domain

import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import java.util.*

enum class ReparationItemType(val func: (UUID, Robot, RobotRepository) -> Unit) : ItemType {
    REPARATION_SWARM(::useRepairSwarm)
}

/**
 * Repairs all [Robots][Robot] of the specified player on the [Planet] the specified Robot is on. All `Robots` heal
 * 20 health. Health can't be healed past `maxHealth`.
 *
 * @param playerId          the `UUID` of the player which owns the specified `Robot`
 * @param robotId           the `UUID` of the `Robot` which should use the item
 * @param robotRepository   the [RobotRepository] containing all Robots. Necessary so all Robots can be correctly saved
 */
private fun useRepairSwarm(playerId: UUID, robot: Robot, robotRepository: RobotRepository) {
    val robots = robotRepository.findAllByPlayerAndPlanet_PlanetId(playerId, robot.planet.planetId)
    robots.forEach {
        it.repairBy(20)
    }
    robotRepository.saveAll(robots)
}

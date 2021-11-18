package com.msd.item.domain

import com.msd.application.GameMapService
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository

enum class MovementItemType(val func: (Robot, RobotRepository, GameMapService) -> Unit) : ItemType {
    WORMHOLE(::useWormhole),
}

fun useWormhole(robot: Robot, repository: RobotRepository, gameMapService: GameMapService) {
    val planetDTO = gameMapService.getAllPlanets().random()
    robot.move(Planet(planetDTO.id), 0)
}

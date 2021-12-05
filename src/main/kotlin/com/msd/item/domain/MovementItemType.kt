package com.msd.item.domain

import com.msd.application.GameMapService
import com.msd.application.dto.GameMapPlanetDto
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository

enum class MovementItemType(val func: (Robot, RobotRepository, GameMapService) -> GameMapPlanetDto) : ItemType {
    WORMHOLE(::useWormhole),
}

private fun useWormhole(robot: Robot, repository: RobotRepository, gameMapService: GameMapService): GameMapPlanetDto {
    val planetDTO = gameMapService.getAllPlanets().random()
    val planet = Planet(planetDTO.id, planetDTO.resource?.resourceType)
    robot.move(planet, 0)
    repository.save(robot)
    return planetDTO
}

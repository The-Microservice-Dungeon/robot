package com.msd.robot.domain

import org.springframework.data.repository.CrudRepository
import java.util.*

interface RobotRepository : CrudRepository<Robot, UUID> {

    fun findAllByAliveFalseAndPlanet_PlanetId(planetId: UUID): List<Robot>

    fun findAllByPlanet_PlanetId(planetId: UUID): List<Robot>

    fun findAllByPlayerAndPlanet_PlanetId(playerId: UUID, planetId: UUID): List<Robot>
}

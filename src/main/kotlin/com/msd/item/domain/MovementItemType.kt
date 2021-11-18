package com.msd.item.domain

import com.msd.application.GameMapService
import com.msd.robot.domain.RobotRepository
import java.util.*

enum class MovementItemType(val func: (UUID, UUID, RobotRepository, GameMapService) -> Unit) : ItemType {
    WORMHOLE(::useWormhole),
}

fun useWormhole(player: UUID, robot: UUID, repository: RobotRepository, gameMapService: GameMapService) {
}

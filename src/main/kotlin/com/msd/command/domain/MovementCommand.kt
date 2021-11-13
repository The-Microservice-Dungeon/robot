package com.msd.command.domain

import java.util.*

/**
 * Describes the request of a player to move a specific [Robot] from one [Planet] to another.
 */
class MovementCommand(
    robotUUID: UUID,
    val playerUUID: UUID,
    val targetPlanetUUID: UUID
) : Command(robotUUID)

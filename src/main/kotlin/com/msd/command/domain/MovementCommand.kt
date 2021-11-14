package com.msd.command.domain

import java.util.*

/**
 * Describes the request of a player to move a specific [Robot] from one [Planet] to another.
 */
class MovementCommand(
    playerUUID: UUID,
    robotUUID: UUID,
    val targetPlanetUUID: UUID,
    transactionUUID: UUID
) : Command(playerUUID, robotUUID, transactionUUID)

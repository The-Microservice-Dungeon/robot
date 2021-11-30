package com.msd.command.application

import java.util.*

/**
 * Describes the request of a player to move a specific [Robot] from one [Planet] to another.
 */
class MovementCommand(
    robotUUID: UUID,
    val targetPlanetUUID: UUID,
    transactionUUID: UUID
) : Command(robotUUID, transactionUUID)

package com.msd.command.domain

import java.util.*

/**
 * Describes the request of a Player to regenerate the energy of a specific [Robot].
 */
class RegenCommand(robotId: UUID, val playerId: UUID) : Command(robotId)

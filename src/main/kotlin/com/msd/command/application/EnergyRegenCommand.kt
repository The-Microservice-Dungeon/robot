package com.msd.command.application

import java.util.*

/**
 * Describes the request of a Player to regenerate the energy of a specific [Robot].
 */
class EnergyRegenCommand(
    playerUUID: UUID,
    robotUUID: UUID,
    transactionUUID: UUID
) :
    Command(playerUUID, robotUUID, transactionUUID)

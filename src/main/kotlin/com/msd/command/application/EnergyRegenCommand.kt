package com.msd.command.application

import java.util.*

/**
 * Describes the request of a Player to regenerate the energy of a specific [Robot].
 */
class EnergyRegenCommand(
    robotUUID: UUID,
    transactionUUID: UUID
) :
    Command(robotUUID, transactionUUID)

package com.msd.command.domain

import java.util.*

/**
 * Describes the request of a Player to regenerate the energy of a specific [Robot].
 */
class RegenCommand(
    playerUUID: UUID,
    robotUUID: UUID,
    transactionUUID: UUID
) :
    Command(playerUUID, robotUUID, transactionUUID)

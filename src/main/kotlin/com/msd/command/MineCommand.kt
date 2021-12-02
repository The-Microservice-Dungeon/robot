package com.msd.command

import com.msd.command.application.command.Command
import com.msd.domain.ResourceType
import java.util.*

/**
 * Describes the request of a player to mine a [ResourceType] with a specific [Robot].
 */
class MineCommand(
    robotUUID: UUID,
    transactionUUID: UUID
) : Command(robotUUID, transactionUUID)

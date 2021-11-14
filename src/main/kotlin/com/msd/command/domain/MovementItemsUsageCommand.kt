package com.msd.command.domain

import com.msd.item.domain.MovementItemType
import java.util.*

class MovementItemsUsageCommand(
    playerUUID: UUID,
    robotUUID: UUID,
    val itemType: MovementItemType,
    transactionUUID: UUID
) :
    Command(playerUUID, robotUUID, transactionUUID)

package com.msd.command.application

import com.msd.item.domain.MovementItemType
import java.util.*

class MovementItemsUsageCommand(
    robotUUID: UUID,
    val itemType: MovementItemType,
    transactionUUID: UUID
) :
    Command(robotUUID, transactionUUID)
package com.msd.command.application

import com.msd.item.domain.RepairItemType
import java.util.*

class RepairItemUsageCommand(
    playerUUID: UUID,
    robotUUID: UUID,
    val itemType: RepairItemType,
    transactionUUID: UUID
) :
    Command(playerUUID, robotUUID, transactionUUID)

package com.msd.command.application

import com.msd.item.domain.RepairItemType
import java.util.*

class RepairItemUsageCommand(
    robotUUID: UUID,
    val itemType: RepairItemType,
    transactionUUID: UUID
) :
    Command(robotUUID, transactionUUID)

package com.msd.command.application

import com.msd.item.domain.AttackItemType
import java.util.*

class AttackItemUsageCommand(
    robotUUID: UUID,
    val itemType: AttackItemType,
    val targetUUID: UUID,
    transactionUUID: UUID
) :
    Command(robotUUID, transactionUUID)

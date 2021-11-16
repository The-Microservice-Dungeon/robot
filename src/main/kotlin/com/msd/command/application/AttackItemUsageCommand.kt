package com.msd.command.application

import com.msd.item.domain.AttackItemType
import java.util.*

class AttackItemUsageCommand(
    playerUUID: UUID,
    robotUUID: UUID,
    val itemType: AttackItemType,
    val targetUUID: UUID,
    transactionUUID: UUID
) :
    Command(playerUUID, robotUUID, transactionUUID)
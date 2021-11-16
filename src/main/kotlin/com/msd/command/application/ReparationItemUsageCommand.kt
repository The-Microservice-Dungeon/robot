package com.msd.command.application

import com.msd.item.domain.ReparationItemType
import java.util.*

class ReparationItemUsageCommand(
    playerUUID: UUID,
    robotUUID: UUID,
    val itemType: ReparationItemType,
    transactionUUID: UUID
) :
    Command(playerUUID, robotUUID, transactionUUID)

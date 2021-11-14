package com.msd.command.domain

import java.util.*

class BlockCommand(
    playerUUID: UUID,
    robotUUID: UUID,
    transactionUUID: UUID
) :
    Command(playerUUID, robotUUID, transactionUUID)

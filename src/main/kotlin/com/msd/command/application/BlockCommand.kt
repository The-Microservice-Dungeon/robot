package com.msd.command.application

import java.util.*

class BlockCommand(
    playerUUID: UUID,
    robotUUID: UUID,
    transactionUUID: UUID
) :
    Command(playerUUID, robotUUID, transactionUUID)

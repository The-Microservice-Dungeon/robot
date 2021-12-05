package com.msd.command.application.command

import java.util.*

class BlockCommand(
    robotUUID: UUID,
    transactionUUID: UUID
) :
    Command(robotUUID, transactionUUID)

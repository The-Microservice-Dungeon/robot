package com.msd.command.application.command

import com.msd.command.application.command.Command
import java.util.*

class BlockCommand(
    robotUUID: UUID,
    transactionUUID: UUID
) :
    Command(robotUUID, transactionUUID)

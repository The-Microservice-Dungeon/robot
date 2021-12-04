package com.msd.command.application.command

import java.util.*

class FightingCommand(robotUUID: UUID, val targetRobotUUID: UUID, transactionUUID: UUID) :
    Command(robotUUID, transactionUUID)

package com.msd.command.domain

import java.util.*

class AttackCommand(playerUUID: UUID, robotUUID: UUID, val targetRobotUUID: UUID, transactionUUID: UUID) :
    Command(playerUUID, robotUUID, transactionUUID)

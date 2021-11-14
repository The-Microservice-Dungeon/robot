package com.msd.command.domain

import java.util.*

class MiningCommand(playerUUID: UUID, robotUUID: UUID, transactionUUID: UUID) :
    Command(playerUUID, robotUUID, transactionUUID)

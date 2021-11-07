package com.msd.command

import java.util.*

class AttackCommand(robotUUID: UUID, val playerUUID: UUID, val targetRobotUUID: UUID) :
    Command(robotUUID)

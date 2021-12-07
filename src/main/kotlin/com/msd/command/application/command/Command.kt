package com.msd.command.application.command

import java.util.*

/**
 * Describes the request of a player to instruct a robot to do a specific task.
 */
open class Command(
    val robotUUID: UUID,
    val transactionUUID: UUID
)

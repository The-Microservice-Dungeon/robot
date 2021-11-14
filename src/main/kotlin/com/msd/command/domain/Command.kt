package com.msd.command.domain

import java.util.*

/**
 * Describes the request of a player to instruct a robot to do a specific task.
 */
open class Command(
    val playerUUID: UUID,
    val robotUUID: UUID,
    val transactionUUID: UUID
)

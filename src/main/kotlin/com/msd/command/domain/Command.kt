package com.msd.command.domain

import java.util.*

/**
 * Describes the request of a player to instruct a robot to do a specific task.
 */
open class Command(
    val robotId: UUID
) {
    val transactionUUID = UUID.randomUUID()
}

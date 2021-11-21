package com.msd.command

import com.msd.domain.ResourceType
import java.util.*

/**
 * Describes the request of a player to mine a [ResourceType] with a specific [Robot].
 */
class MineCommand(
    robotUUID: UUID,
    val playerUUID: UUID,
    val resourceType: ResourceType
) : Command(robotUUID)
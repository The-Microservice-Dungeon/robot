package com.msd.event.application.dto

import java.util.*

class BlockEventDTO(
    success: Boolean,
    message: String,
    val planetId: UUID?,
    val remainingEnergy: Int?
) : EventDTO(success, message)

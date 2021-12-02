package com.msd.application.dto

import com.msd.application.EventType
import java.util.*

class BlockEventDTO(
    success: Boolean,
    message: String,
    val planetId: UUID?,
    val remainingEnergy: Int?
) : EventDTO(success, message) {
}
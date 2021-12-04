package com.msd.application.dto

import java.util.*

class ItemFightingEventDTO(
    success: Boolean,
    message: String,
    val remainingItemCount: Int?,
    val associatedFights: List<UUID> = mutableListOf()
) : EventDTO(success, message)

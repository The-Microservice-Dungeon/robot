package com.msd.application.dto

import com.msd.application.EventType

open class EventDTO(val success: Boolean, val message: String, val eventType: EventType)

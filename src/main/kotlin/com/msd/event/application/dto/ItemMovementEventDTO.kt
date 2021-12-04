package com.msd.event.application.dto

class ItemMovementEventDTO(success: Boolean, message: String, val associatedMovement: String?) : EventDTO(success, message)

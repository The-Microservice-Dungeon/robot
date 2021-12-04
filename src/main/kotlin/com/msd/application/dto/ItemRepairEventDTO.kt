package com.msd.application.dto

class ItemRepairEventDTO(success: Boolean, message: String, val robots: List<RepairEventRobotDTO>) : EventDTO(success, message)

package com.msd.robot.application.dtos

import com.msd.robot.application.RestorationType
import java.util.*

data class RestorationDTO(
    val transactionId: UUID,
    val restorationType: RestorationType
)

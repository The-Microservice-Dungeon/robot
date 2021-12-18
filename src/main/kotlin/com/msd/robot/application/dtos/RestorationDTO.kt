package com.msd.robot.application.dtos

import com.msd.robot.application.RestorationType
import java.util.*

data class RestorationDTO(
    val transactionID: UUID,
    val restorationType: RestorationType
)

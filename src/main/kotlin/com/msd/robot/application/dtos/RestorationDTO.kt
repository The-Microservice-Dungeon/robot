package com.msd.robot.application.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import com.msd.robot.application.RestorationType
import java.util.*

data class RestorationDTO(
    @JsonProperty("transaction-id")val transactionID: UUID,
    @JsonProperty("restoration-type") val restorationType: RestorationType
)

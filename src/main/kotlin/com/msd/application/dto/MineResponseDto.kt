package com.msd.application.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class MineResponseDto(
    val amount_mined: Int
)

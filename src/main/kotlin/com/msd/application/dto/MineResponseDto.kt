package com.msd.application.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties
class MineResponseDto(
    val amount_mined: Int
)

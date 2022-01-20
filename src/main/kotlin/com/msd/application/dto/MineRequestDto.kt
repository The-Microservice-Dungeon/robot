package com.msd.application.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties
class MineRequestDto(
    amount: Int
) {
    val mining: MiningDto = MiningDto(amount)

    class MiningDto(
        amount_requested: Int
    )
}

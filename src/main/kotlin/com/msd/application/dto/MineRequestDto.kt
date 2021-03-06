package com.msd.application.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class MineRequestDto(
    amount: Int
) {
    val mining: MiningDto = MiningDto(amount)

    class MiningDto(
        val amount_requested: Int
    )
}

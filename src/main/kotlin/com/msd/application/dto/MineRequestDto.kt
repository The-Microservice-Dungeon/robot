package com.msd.application.dto

class MineRequestDto(
    amount: Int
) {
    val mining: MiningDto = MiningDto(amount)

    class MiningDto(
        amount_requested: Int
    )
}

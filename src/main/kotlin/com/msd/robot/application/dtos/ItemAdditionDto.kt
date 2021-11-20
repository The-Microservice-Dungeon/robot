package com.msd.robot.application.dtos

import com.msd.item.domain.ItemType
import java.util.*

class ItemAdditionDto(
    val transaction_id: UUID,
    val `item-type`: ItemType
)

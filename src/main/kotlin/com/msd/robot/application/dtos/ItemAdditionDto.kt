package com.msd.robot.application.dtos

import com.msd.item.domain.ItemType
import java.util.*

class ItemAdditionDto(
    val transactionId: UUID,
    val itemType: ItemType
)

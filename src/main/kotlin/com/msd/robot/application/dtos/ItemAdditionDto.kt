package com.msd.robot.application.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import com.msd.item.domain.ItemType
import java.util.*

class ItemAdditionDto(
    val transactionId: UUID,

    @JsonProperty("itemType")
    val itemType: ItemType
)

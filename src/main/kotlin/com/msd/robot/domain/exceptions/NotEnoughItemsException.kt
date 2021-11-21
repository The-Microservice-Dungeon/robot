package com.msd.robot.domain.exceptions

import com.msd.item.domain.ItemType

class NotEnoughItemsException(s: String, missingItem: ItemType) :
    RuntimeException("$s\n Missing item: $missingItem")

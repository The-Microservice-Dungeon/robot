package com.msd.robot.domain.exception

import com.msd.core.FailureException
import com.msd.item.domain.ItemType

class NotEnoughItemsException(s: String, missingItem: ItemType) :
    FailureException("$s\n Missing item: $missingItem")

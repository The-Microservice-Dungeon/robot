package com.msd.item.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.msd.application.ItemTypeDeserializer

/**
 * An interface used so that all [ItemTypes][item.domain] can be referred to by the same Type.
 */
@JsonDeserialize(using = ItemTypeDeserializer::class)
interface ItemType

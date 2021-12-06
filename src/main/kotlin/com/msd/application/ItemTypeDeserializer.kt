package com.msd.application

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.msd.item.domain.AttackItemType
import com.msd.item.domain.ItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.RepairItemType
import java.lang.RuntimeException

class ItemTypeDeserializer() : StdDeserializer<ItemType>(ItemType::class.java) {

    override fun deserialize(parser: JsonParser, context: DeserializationContext): ItemType {
        val itemTypeString = parser.text.trim()
        try {
            return AttackItemType.valueOf(itemTypeString)
        } catch (e: Exception) { }
        try {
            return MovementItemType.valueOf(itemTypeString)
        } catch (e: Exception) {}
        try {
            return RepairItemType.valueOf(itemTypeString)
        } catch (e: Exception) {}
        throw RuntimeException("Unknown Item Type: $itemTypeString")
    }
}

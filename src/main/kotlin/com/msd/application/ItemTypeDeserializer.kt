package com.msd.application

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
/*import com.msd.item.domain.AttackItemType
import com.msd.item.domain.ItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.RepairItemType

import mu.KotlinLogging
import java.lang.RuntimeException

class ItemTypeDeserializer : StdDeserializer<ItemType>(ItemType::class.java) {

    private val logger = KotlinLogging.logger {}

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
        logger.info("Failed to deserialize ItemType: $itemTypeString")
        throw RuntimeException("Unknown Item Type: $itemTypeString")
    }
}
*/
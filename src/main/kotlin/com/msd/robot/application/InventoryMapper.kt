package com.msd.robot.application

import com.msd.domain.ResourceType
import com.msd.robot.application.dtos.InventoryDto
import com.msd.robot.domain.Inventory
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

@Mapper(imports = [ResourceType::class])
interface InventoryMapper {

    @Mappings(
        Mapping(target = "storedCoal", expression = "java(inventory.getStorageUsageForResource(ResourceType.COAL))"),
        Mapping(target = "storedIron", expression = "java(inventory.getStorageUsageForResource(ResourceType.IRON))"),
        Mapping(target = "storedGem", expression = "java(inventory.getStorageUsageForResource(ResourceType.GEM))"),
        Mapping(target = "storedGold", expression = "java(inventory.getStorageUsageForResource(ResourceType.GOLD))"),
        Mapping(target = "storedPlatin", expression = "java(inventory.getStorageUsageForResource(ResourceType.PLATIN))")
    )
    fun inventoryToInventoryDto(inventory: Inventory): InventoryDto
}

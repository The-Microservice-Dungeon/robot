package com.msd.robot.application.mappers
/*
import com.msd.item.domain.AttackItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.RepairItemType
import com.msd.robot.application.dtos.ItemsDto
*/
import com.msd.robot.application.dtos.RobotDto
import com.msd.robot.domain.Inventory
import com.msd.robot.domain.Robot
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

@Mapper(uses = [InventoryMapper::class])
abstract class RobotMapper {

    @Mappings(
        Mapping(target = "planet", source = "planet.planetId"),
      //  Mapping(target = "items", expression = "java(getItems(robot.getInventory()))"),
        Mapping(target = "storageLevel", source = "robot.inventory.storageLevel")
    )
    abstract fun robotToRobotDto(robot: Robot): RobotDto

    abstract fun robotsToRobotDtos(robots: List<Robot>): List<RobotDto>

 /*   fun getItems(inventory: Inventory): ItemsDto {
        return ItemsDto(
            inventory.getItemAmountByType(AttackItemType.ROCKET),
            inventory.getItemAmountByType(MovementItemType.WORMHOLE),
            inventory.getItemAmountByType(AttackItemType.LONG_RANGE_BOMBARDMENT),
            inventory.getItemAmountByType(AttackItemType.SELF_DESTRUCTION),
            inventory.getItemAmountByType(RepairItemType.REPAIR_SWARM),
            inventory.getItemAmountByType(AttackItemType.NUKE),
        )
    }
    */
}

package com.msd.robot.application.mappers

import com.msd.domain.ResourceType
//import com.msd.item.domain.AttackItemType
//import com.msd.item.domain.MovementItemType
//import com.msd.item.domain.RepairItemType
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.*

@SpringBootTest
@ActiveProfiles(profiles = ["test"])
internal class RobotMapperTest(
    @Autowired
    val robotMapper: RobotMapper
) {
    private lateinit var robot: Robot

    @BeforeEach
    fun init() {
        robot = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robot.inventory.addResource(ResourceType.COAL, 1)
        robot.inventory.addResource(ResourceType.IRON, 2)
        robot.inventory.addResource(ResourceType.GEM, 3)
        robot.inventory.addResource(ResourceType.GOLD, 4)
        robot.inventory.addResource(ResourceType.PLATIN, 5)
     /*   robot.inventory.addItem(MovementItemType.WORMHOLE)
        robot.inventory.addItem(RepairItemType.REPAIR_SWARM)
        robot.inventory.addItem(AttackItemType.NUKE)
        robot.inventory.addItem(AttackItemType.ROCKET)
        robot.inventory.addItem(AttackItemType.LONG_RANGE_BOMBARDMENT)
        robot.inventory.addItem(AttackItemType.SELF_DESTRUCTION)
        */
    }

    @Test
    fun `when converting a robot to RobotDTO all values are correctly set`() {
        val robotDto = robotMapper.robotToRobotDto(robot)

        assertAll(
            "All values correctly mapped",
            {
                assertEquals(robot.id, robotDto.id)
            },
            {
                assertEquals(robot.alive, robotDto.alive)
            },
            {
                assertEquals(1, robotDto.inventory.storedCoal)
            },
            {
                assertEquals(2, robotDto.inventory.storedIron)
            },
            {
                assertEquals(3, robotDto.inventory.storedGem)
            },
            {
                assertEquals(4, robotDto.inventory.storedGold)
            },
            {
                assertEquals(5, robotDto.inventory.storedPlatin)
            }
        )
    }
}

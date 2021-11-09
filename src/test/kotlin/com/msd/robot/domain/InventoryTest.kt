package com.msd.robot.domain

import com.msd.domain.ResourceType
import com.msd.planet.domain.Planet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.util.*

class InventoryTest {

    private lateinit var robot1: Robot

    @BeforeEach
    fun initialize() {
        robot1 = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
    }

    @Test
    fun `Used storage can't exceed max storage`() {
        // then
        assertThrows<InventoryFullException>("Added resources exceed maxStorage. Would be 25, max is 20") {
            robot1.inventory.addResource(ResourceType.COAL, 25)
        }
        assertThrows<InventoryFullException>("Added resources exceed maxStorage. Would be 30, max is 20") {
            robot1.inventory.addResource(ResourceType.IRON, 10)
        }
        assertEquals(20, robot1.inventory.usedStorage)
        assertEquals(20, robot1.inventory.getStorageUsageForResource(ResourceType.COAL))
        assertEquals(0, robot1.inventory.getStorageUsageForResource(ResourceType.IRON))
    }

    @Test
    fun `Used storage is correct when holding different resources`() {
        // when
        robot1.inventory.addResource(ResourceType.COAL, 5)
        robot1.inventory.addResource(ResourceType.IRON, 10)
        robot1.inventory.addResource(ResourceType.PLATIN, 5)
        // then
        assertEquals(20, robot1.inventory.usedStorage)
    }

    @Test
    fun `Inventory can hold multiple resource`() {
        // when
        robot1.inventory.addResource(ResourceType.COAL, 5)
        robot1.inventory.addResource(ResourceType.IRON, 10)
        robot1.inventory.addResource(ResourceType.PLATIN, 5)
        // then
        assertAll(
            {
                assertEquals(5, robot1.inventory.getStorageUsageForResource(ResourceType.COAL))
            },
            {
                assertEquals(10, robot1.inventory.getStorageUsageForResource(ResourceType.IRON))
            },
            {
                assertEquals(5, robot1.inventory.getStorageUsageForResource(ResourceType.PLATIN))
            }
        )
    }

    @Test
    fun `Removing resource correctly reduces used storage`() {
        // given
        robot1.inventory.addResource(ResourceType.COAL, 5)
        robot1.inventory.addResource(ResourceType.IRON, 10)
        robot1.inventory.addResource(ResourceType.PLATIN, 5)

        // when
        robot1.inventory.takeResource(ResourceType.COAL, 3)
        robot1.inventory.takeResource(ResourceType.IRON, 1)
        robot1.inventory.takeResource(ResourceType.PLATIN, 4)

        // then
        assertEquals(12, robot1.inventory.usedStorage)
        assertAll(
            {
                assertEquals(2, robot1.inventory.getStorageUsageForResource(ResourceType.COAL))
            },
            {
                assertEquals(9, robot1.inventory.getStorageUsageForResource(ResourceType.IRON))
            },
            {
                assertEquals(1, robot1.inventory.getStorageUsageForResource(ResourceType.PLATIN))
            }
        )
    }

    @Test
    fun `Can't take more resources than available`() {
        // given
        robot1.inventory.addResource(ResourceType.IRON, 10)
        // when
        assertThrows<NotEnoughResourcesException>("Wanted to take 15, but only 10 were available") {
            robot1.inventory.takeResource(ResourceType.IRON, 15)
        }

        assertEquals(10, robot1.inventory.getStorageUsageForResource(ResourceType.IRON))
        assertEquals(10, robot1.inventory.usedStorage)
    }

    @Test
    fun `Upgrading storage allows robot to hold more resources`() {
        // given
        robot1.upgrade(UpgradeType.STORAGE, 1)
        // when
        robot1.inventory.addResource(ResourceType.IRON, 25)
        // then
        assertEquals(50, robot1.inventory.maxStorage)
        assertEquals(25, robot1.inventory.getStorageUsageForResource(ResourceType.IRON))
    }
}

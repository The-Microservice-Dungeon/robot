package com.msd.robot.domain

import com.msd.domain.ResourceType
import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Inventory {
    @Id
    val id = UUID.randomUUID()
    var storageLevel: Int = 0
        internal set(value) {
            if (value > 5) throw UpgradeException("Max Storage Level has been reached. Upgrade not possible.")
            else if (value > storageLevel + 1)
                throw UpgradeException(
                    "Cannot skip upgrade levels. Tried to upgrade from level $storageLevel to level $value"
                )
            else if (value <= storageLevel)
                throw UpgradeException("Cannot downgrade Robot. Tried to go from level $storageLevel to level $value")
            field = value
        }
    val maxStorage
        get() = UpgradeValues.storageByLevel[storageLevel]
    var usedStorage = 0
        private set

    @ElementCollection
    private val resourceMap = mutableMapOf(
        ResourceType.COAL to 0,
        ResourceType.IRON to 0,
        ResourceType.GEM to 0,
        ResourceType.GOLD to 0,
        ResourceType.PLATIN to 0,
    )

    /**
     * Adds a resource to this inventory. The inventory can hold all resources simultaneously, but the amount of
     * resources held cannot exceed <code>maxStorage</code>.
     *
     * @param resource  the resource which will be added to the inventory
     * @param amount    the amount that will be added
     */
    fun addResource(resource: ResourceType, amount: Int) {
        val newUsedStorage = usedStorage + amount
        if (newUsedStorage > maxStorage) {
            resourceMap[resource] = resourceMap[resource]!! + amount - (newUsedStorage - maxStorage)
            usedStorage = maxStorage
            throw InventoryFullException("Added resources exceed maxStorage. Would be $newUsedStorage, max is $maxStorage")
        } else {
            resourceMap[resource] = resourceMap[resource]!! + amount
            usedStorage += amount
        }
    }

    /**
     * Returns the stored amount of a given resource. The resource will still remain in the inventory
     *
     * @param resource  the resource of which the amount should be returned
     * @return the stored amount of the resource as an <code>Int</code>
     */
    fun getStorageUsageForResource(resource: ResourceType): Int {
        return resourceMap[resource]!!
    }

    /**
     * Takes a specified amount of resources from this inventory. The resources will be removed from the inventory.
     *
     * @param resource  the resource which should be taken
     * @param amount    the amount which should be taken
     */
    fun takeResource(resource: ResourceType, amount: Int) {
        if (resourceMap[resource]!! < amount) throw NotEnoughResourcesException("Wanted to take $amount, but only ${resourceMap[resource]!!} 10 were available")
        resourceMap[resource] = resourceMap[resource]!! - amount
        usedStorage -= amount
    }

    /**
     * Takes all of the specified resource from the inventory
     *
     * @param resource The resource type to empty
     * @return The number of resources of that type taken
     */
    fun takeAllResourcesOfType(resource: ResourceType): Int {
        val amount = getStorageUsageForResource(resource)
        takeResource(resource, amount)
        return amount
    }
}

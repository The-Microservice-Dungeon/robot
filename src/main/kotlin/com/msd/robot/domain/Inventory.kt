package com.msd.robot.domain

import com.msd.domain.ResourceType
import com.msd.item.domain.AttackItemType
import com.msd.item.domain.ItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.RepairItemType
import com.msd.robot.domain.exception.InventoryFullException
import com.msd.robot.domain.exception.NotEnoughResourcesException
import com.msd.robot.domain.exception.UpgradeException
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
class Inventory {
    @Id
    @Type(type="uuid-char")
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

    @ElementCollection(fetch = FetchType.EAGER)
    private val resourceMap = mutableMapOf(
        ResourceType.COAL to 0,
        ResourceType.IRON to 0,
        ResourceType.GEM to 0,
        ResourceType.GOLD to 0,
        ResourceType.PLATIN to 0,
    )

    private var wormholeAmount = 0
        set(value) {
            if (value < 0) throw IllegalArgumentException("Can't set ItemAmount below 0")
            field = value
        }
    private var repairSwarmAmount = 0
        set(value) {
            if (value < 0) throw IllegalArgumentException("Can't set ItemAmount below 0")
            field = value
        }
    private var rocketAmount = 0
        set(value) {
            if (value < 0) throw IllegalArgumentException("Can't set ItemAmount below 0")
            field = value
        }
    private var bombardmentAmount = 0
        set(value) {
            if (value < 0) throw IllegalArgumentException("Can't set ItemAmount below 0")
            field = value
        }
    private var selfDestructAmount = 0
        set(value) {
            if (value < 0) throw IllegalArgumentException("Can't set ItemAmount below 0")
            field = value
        }
    private var nukeAmount = 0
        set(value) {
            if (value < 0) throw IllegalArgumentException("Can't set ItemAmount below 0")
            field = value
        }

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
     * Removes all resources from the inventory.
     *
     * @return a list of all the resources taken
     */
    fun takeAllResources(): MutableMap<ResourceType, Int> {
        val takenResources = mutableMapOf<ResourceType, Int>()
        ResourceType.values().forEach {
            val amount = resourceMap[it]!!
            takenResources[it] = amount
            usedStorage -= amount
            resourceMap[it] = 0
        }
        return takenResources
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

    /**
     * Checks if the `Inventory` of this `Robot` is full.
     *
     * @return `true` if the inventory is full, otherwise `false`
     */
    fun isFull(): Boolean {
        return maxStorage - usedStorage == 0
    }

    /**
     * Adds one special item to this `Inventory`. There is no limit to how many items an `Inventory` can hold.
     *
     * @param item    the type of the item that should be added
     * @throws IllegalArgumentException    when the passed `ItemType` is invalid
     */
    fun addItem(item: ItemType) {
        when (item) {
            MovementItemType.WORMHOLE -> wormholeAmount++
            RepairItemType.REPAIR_SWARM -> repairSwarmAmount++
            AttackItemType.ROCKET -> rocketAmount++
            AttackItemType.LONG_RANGE_BOMBARDMENT -> bombardmentAmount++
            AttackItemType.SELF_DESTRUCTION -> selfDestructAmount++
            AttackItemType.NUKE -> nukeAmount++
            else -> throw IllegalArgumentException("$item is not a valid item")
        }
    }

    /**
     * Removes one item from this `Inventory`. The amount of items held cannot be smaller than 0.
     *
     * @param item    the Type of the item that should be removed
     * @throws IllegalArgumentException    when the passed `ItemType` is invalid
     */
    fun removeItem(item: ItemType) {
        when (item) {
            MovementItemType.WORMHOLE -> wormholeAmount--
            RepairItemType.REPAIR_SWARM -> repairSwarmAmount--
            AttackItemType.ROCKET -> rocketAmount--
            AttackItemType.LONG_RANGE_BOMBARDMENT -> bombardmentAmount--
            AttackItemType.SELF_DESTRUCTION -> selfDestructAmount--
            AttackItemType.NUKE -> nukeAmount--
            else -> throw IllegalArgumentException("$item is not a valid item")
        }
    }

    /**
     * Returns the held amount of the specified `ItemType`.
     *
     * @param item    the type of the item of which the amount should be retrieved
     * @return the amount of the specified item held as an `Int`
     * @throws IllegalArgumentException    when the passed `ItemType` is invalid
     */
    fun getItemAmountByType(item: ItemType): Int {
        return when (item) {
            MovementItemType.WORMHOLE -> wormholeAmount
            RepairItemType.REPAIR_SWARM -> repairSwarmAmount
            AttackItemType.ROCKET -> rocketAmount
            AttackItemType.LONG_RANGE_BOMBARDMENT -> bombardmentAmount
            AttackItemType.SELF_DESTRUCTION -> selfDestructAmount
            AttackItemType.NUKE -> nukeAmount
            else -> throw IllegalArgumentException("$item is not a valid item")
        }
    }
}

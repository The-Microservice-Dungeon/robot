package com.msd.robot.domain

import com.msd.admin.application.EnergyCostCalculationVerbs
import com.msd.admin.application.GameplayVariablesLevelVerbs
import com.msd.domain.ResourceType
import com.msd.planet.domain.Planet
import com.msd.robot.domain.exception.NotEnoughEnergyException
import com.msd.robot.domain.exception.PlanetBlockedException
import com.msd.robot.domain.exception.UpgradeException
import com.msd.robot.domain.gameplayVariables.*
import org.hibernate.annotations.Type
import java.lang.IllegalArgumentException
import java.util.*
import javax.persistence.*
import kotlin.math.round

// TODO("Integrate EnergyCostCalculationObject")

fun Map<GameplayVariablesLevelVerbs, Int>.getByVal(value: Int): Int {
    return when (value) {
        0 -> this[GameplayVariablesLevelVerbs.LVL0]!!
        1 -> this[GameplayVariablesLevelVerbs.LVL1]!!
        2 -> this[GameplayVariablesLevelVerbs.LVL2]!!
        3 -> this[GameplayVariablesLevelVerbs.LVL3]!!
        4 -> this[GameplayVariablesLevelVerbs.LVL4]!!
        5 -> this[GameplayVariablesLevelVerbs.LVL5]!!

        else -> throw IndexOutOfBoundsException()
    }
}

@Entity
class Robot(
    @Type(type = "uuid-char")
    val player: UUID,
    planet: Planet,
    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER)
    val upgradeValues: UpgradeValues = UpgradeValues(),
    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER)
    val energyCostCalculationValues: EnergyCostCalculationValues = EnergyCostCalculationValues()
) {
    @Id
    @Type(type = "uuid-char")
    val id: UUID = UUID.randomUUID()

    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER)
    var planet = planet
        private set

    var alive: Boolean = true

    val maxHealth
        get() = upgradeValues.healthValues.getByVal(healthLevel)

    val maxEnergy
        get() = upgradeValues.energyCapacityValues.getByVal(energyLevel)

    val energyRegen
        get() = upgradeValues.energyRegenerationValues.getByVal(energyRegenLevel)

    val attackDamage: Int
        get() = upgradeValues.damageValues.getByVal(damageLevel)

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    val inventory = Inventory(upgradeValues)

    val miningSpeed: Int
        get() = upgradeValues.miningSpeedValues.getByVal(miningSpeedLevel)

    var health: Int = maxHealth
        private set(value) {
            field = if (value > this.maxHealth)
                maxHealth
            else
                value
        }

    var energy: Int = maxEnergy
        private set(value) {
            field = if (value > maxEnergy)
                maxEnergy
            else
                value
        }

    /**
     * Sets the robot's healthLevel to the given value.
     *
     * @throws UpgradeException    when an upgrade level is skipped, a downgrade is attempted or an upgrade past the max
     */
    var healthLevel: Int = 0
        private set(value) {
            if (value > 5) throw UpgradeException("Max Health Level has been reached. Upgrade not possible.")
            else if (value > healthLevel + 1)
                throw UpgradeException(
                    "Cannot skip upgrade levels. Tried to upgrade from level $healthLevel to level $value"
                )
            else if (value <= healthLevel)
                throw UpgradeException("Cannot downgrade Robot. Tried to go from level $healthLevel to level $value")
            val diff = upgradeValues!!.healthValues.getByVal(value) - upgradeValues!!.healthValues.getByVal(field)
            field = value
            health += diff
        }

    /**
     * Sets the robot's damageLevel to the given value.
     *
     * @throws UpgradeException when an upgrade level is skipped, a downgrade is attempted or an upgrade past the max
     */
    var damageLevel: Int = 0
        private set(value) {
            if (value > 5) throw UpgradeException("Max Damage Level has been reached. Upgrade not possible.")
            else if (value > damageLevel + 1)
                throw UpgradeException(
                    "Cannot skip upgrade levels. " +
                        "Tried to upgrade from level $damageLevel to level $value"
                )
            else if (value <= damageLevel)
                throw UpgradeException("Cannot downgrade Robot. Tried to go from level $damageLevel to level $value")
            field = value
        }

    /**
     * Sets the robot's miningSpeedLevel to the given value.
     *
     * @throws UpgradeException when an upgrade level is skipped, a downgrade is attempted or an upgrade past the max
     */
    var miningSpeedLevel: Int = 0
        private set(value) {
            if (value > 5) throw UpgradeException("Max MiningSpeed Level has been reached. Upgrade not possible.")
            else if (value > miningSpeedLevel + 1)
                throw UpgradeException(
                    "Cannot skip upgrade levels. Tried to upgrade from level $miningSpeedLevel to level $value"
                )
            else if (value <= miningSpeedLevel)
                throw UpgradeException("Cannot downgrade Robot. Tried to go from level $miningSpeedLevel to level $value")
            field = value
        }

    /**
     * Sets the robot's miningLevel to the given value.
     *
     * @throws UpgradeException when an upgrade level is skipped, a downgrade is attempted or an upgrade past the max
     */
    var miningLevel: Int = 0
        private set(value) {
            if (value > 4) throw UpgradeException("Max Mining Level has been reached. Upgrade not possible.")
            else if (value > miningLevel + 1)
                throw UpgradeException(
                    "Cannot skip upgrade levels. Tried to upgrade from level $miningLevel to level $value"
                )
            else if (value <= miningLevel)
                throw UpgradeException("Cannot downgrade Robot. Tried to go from level $miningLevel to level $value")
            field = value
        }

    /**
     * Sets the robot's energyLevel to the given value.
     *
     * @throws UpgradeException when an upgrade level is skipped, a downgrade is attempted or an upgrade past the max
     */
    var energyLevel: Int = 0
        private set(value) {
            if (value > 5) throw UpgradeException("Max Energy Level has been reached. Upgrade not possible.")
            else if (value > energyLevel + 1)
                throw UpgradeException(
                    "Cannot skip upgrade levels. Tried to upgrade from level $energyLevel to level $value"
                )
            else if (value <= energyLevel)
                throw UpgradeException("Cannot downgrade Robot. Tried to go from level $energyLevel to level $value")
            val diff = upgradeValues.energyCapacityValues.getByVal(value) - upgradeValues.energyCapacityValues.getByVal(field)
            field = value
            energy += diff
        }

    /**
     * Sets the robot's energyRegenLevel to the given value.
     *
     * @throws UpgradeException when an upgrade level is skipped, a downgrade is attempted or an upgrade past the max
     */
    var energyRegenLevel: Int = 0
        private set(value) {
            if (value > 5) throw UpgradeException("Max Energy Regen Level has been reached. Upgrade not possible.")
            else if (value > energyRegenLevel + 1)
                throw UpgradeException(
                    "Cannot skip upgrade levels. Tried to upgrade from level $energyRegenLevel to level $value"
                )
            else if (value <= energyRegenLevel)
                throw UpgradeException("Cannot downgrade Robot. Tried to go from level $energyRegenLevel to level $value")
            field = value
        }

    /**
     * Moves this `Robot` to a given `Planet`, unless the `Robots` current `Planet` is blocked.
     *
     * @param planet The `Planet` to move to.
     * @param cost The cost of moving to the `Planet`, which is the movement difficulty of the target.
     * @throws PlanetBlockedException if the current `Planet` is blocked.
     * @throws NotEnoughEnergyException if the `Robot` does not have enough energy to move to the `Planet`.
     */
    fun move(planet: Planet, cost: Int) {
        this.reduceEnergy(round(energyCostCalculationValues.energyCostValues[EnergyCostCalculationVerbs.MOVEMENT_MULTIPLIER]!! * cost).toInt())
        if (this.planet.blocked)
            throw PlanetBlockedException("Can't move out of a blocked planet")
        this.planet = planet
    }

    /**
     * Blocks the current planet.
     *
     * @throws NotEnoughEnergyException if the robot has not enough energy to block
     */
    fun block() {
        this.reduceEnergy(round(energyCostCalculationValues.energyCostValues[EnergyCostCalculationVerbs.BLOCKING_BASE_COST]!! + (energyCostCalculationValues.energyCostValues[EnergyCostCalculationVerbs.BLOCKING_MAX_ENERGY_PROPORTION]!! * maxEnergy)).toInt())
        this.planet.blocked = true
    }

    /**
     * This `Robot` receives a given amount of [damage].
     * If the damage reduces the health to (or below) 0, the robot is destroyed.
     *
     * @param damage the amount of damage to be received
     */
    fun receiveDamage(damage: Int) {
        this.health -= damage
        if (health <= 0) alive = false
    }

    /**
     * Reduces the energy of the `Robot` by the given amount.
     *
     * @param amount the amount of energy to be subtracted
     * @throws NotEnoughEnergyException if the `Robots` current energy is less than the amount to be subtracted
     * @throws IllegalArgumentException if the amount to be subtracted is negative
     */
    private fun reduceEnergy(amount: Int) {
        if (amount > energy) throw NotEnoughEnergyException("Tried to reduce energy by $amount but only has $energy energy")
        if (amount < 0) throw IllegalArgumentException("Used energy amount cannot be less than zero")

        energy -= amount
    }

    /**
     * This `Robot` attacks another `Robot`.
     *
     * @param otherRobot the [Robot] to attack
     * @throws NotEnoughEnergyException if the robot has not enough energy to attack
     */
    fun attack(otherRobot: Robot) {
        reduceEnergy(round(energyCostCalculationValues.energyCostValues[EnergyCostCalculationVerbs.ATTACKING_MULTIPLIER]!! * (damageLevel + energyCostCalculationValues.energyCostValues[EnergyCostCalculationVerbs.ATTACKING_WEIGHT]!!)).toInt())
        otherRobot.receiveDamage(attackDamage)
    }

    /**
     * Changes the level of an Upgrade of this `Robot`. Upgrade levels can't be skipped, downgrades are not allowed and
     * the max level for all upgrades is 5 except for `Mining` which is 4. If one of those cases appears an Exception
     * is thrown.
     *
     * @param upgradeType the stat which should be updated
     * @param level       the level to which an upgrade should be upgraded
     * @throws UpgradeException    when an upgrade level is skipped, a downgrade is attempted or an upgrade past the max
     */
    fun upgrade(upgradeType: UpgradeType, level: Int) {
        when (upgradeType) {
            UpgradeType.DAMAGE -> damageLevel = level
            UpgradeType.ENERGY_REGEN -> energyRegenLevel = level
            UpgradeType.MAX_ENERGY -> energyLevel = level
            UpgradeType.HEALTH -> healthLevel = level
            UpgradeType.STORAGE -> inventory.storageLevel = level
            UpgradeType.MINING_SPEED -> miningSpeedLevel = level
            UpgradeType.MINING -> miningLevel = level
        }
    }

    /**
     * Checks if this robot has a high enough level to mine the given resource.
     *
     * @param resourceType the type of resource which should be mined
     * @return if this robot can mine the given [resourceType]
     */
    fun canMine(resourceType: ResourceType): Boolean {
        return this.miningLevel >= resourceType.requiredMiningLevel
    }

    /**
     * @return the sum of all upgrades of this this [Robot]
     */
    fun totalUpgrades(): Int {
        return damageLevel + energyLevel + energyRegenLevel + healthLevel + inventory.storageLevel + miningSpeedLevel + miningLevel
    }

    /**
     * Regenerates this [Robot's] [Robot] `energy`. The amount restored corresponds to the `energyRegen` value.
     */
    fun regenerateEnergy() {
        energy += energyRegen
    }

    /**
     * Fully restores a `Robot's` `energy`. This method unlike `regenerateEnergy` is supposed to represent the energy
     * regeneration at a space station.
     */
    fun restoreEnergy() {
        energy = maxEnergy
    }

    /**
     * Repairs this [Robot] to full health.
     */
    fun repair() {
        this.health = this.maxHealth
    }

    fun repairBy(repairAmount: Int) {
        this.health += repairAmount
    }
}

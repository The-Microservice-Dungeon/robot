package com.msd.robot.domain

import com.msd.domain.ResourceType
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetType
import java.lang.IllegalArgumentException
import java.util.*
import javax.persistence.*
import kotlin.math.round

object UpgradeValues {
    val storageByLevel = arrayOf(20, 50, 100, 200, 400, 1000)
    val maxHealthByLevel = arrayOf(10, 25, 50, 100, 200, 500)
    val attackDamageByLevel = arrayOf(1, 2, 5, 10, 20, 50)
    val miningSpeedByLevel = arrayOf(2, 5, 10, 15, 20, 40)
    val maxEnergyByLevel = arrayOf(20, 30, 40, 60, 100, 200)
    val energyRegenByLevel = arrayOf(4, 6, 8, 10, 15, 20)
}

@Entity
class Robot(
    val player: UUID,
    planet: Planet
) {
    @Id
    val id: UUID = UUID.randomUUID()

    @ManyToOne(cascade = [CascadeType.MERGE])
    var planet = planet
        private set

    var alive: Boolean = true

    val maxHealth
        get() = UpgradeValues.maxHealthByLevel[healthLevel]

    val maxEnergy
        get() = UpgradeValues.maxEnergyByLevel[energyLevel]

    val energyRegen
        get() = UpgradeValues.energyRegenByLevel[energyRegenLevel]

    val attackDamage: Int
        get() = UpgradeValues.attackDamageByLevel[damageLevel]

    @OneToOne(cascade = [CascadeType.ALL])
    val inventory = Inventory()
    val miningSpeed: Int
        get() = UpgradeValues.miningSpeedByLevel[miningSpeedLevel]

    var health: Int = maxHealth
        private set(value) {
            if (value > this.maxHealth) throw HealthFullException("Tried to repair robot but it is already at full health.")
            field = value
        }

    var energy: Int = maxEnergy
        private set(value) {
            field = if (value > maxEnergy)
                maxEnergy
            else
                value
        }

    var healthLevel: Int = 0
        private set(value) {
            if (value > 5) throw UpgradeException("Max Health Level has been reached. Upgrade not possible.")
            else if (value > healthLevel + 1)
                throw UpgradeException(
                    "Cannot skip upgrade levels. Tried to upgrade from level $healthLevel to level $value"
                )
            else if (value <= healthLevel)
                throw UpgradeException("Cannot downgrade Robot. Tried to go from level $healthLevel to level $value")
            field = value
        }

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

    var energyLevel: Int = 0
        private set(value) {
            if (value > 5) throw UpgradeException("Max Energy Level has been reached. Upgrade not possible.")
            else if (value > energyLevel + 1)
                throw UpgradeException(
                    "Cannot skip upgrade levels. Tried to upgrade from level $energyLevel to level $value"
                )
            else if (value <= energyLevel)
                throw UpgradeException("Cannot downgrade Robot. Tried to go from level $energyLevel to level $value")
            field = value
        }

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

    fun move(planet: Planet, cost: Int) {
        this.reduceEnergy(cost)
        if (this.planet.blocked)
            throw PlanetBlockedException("Can't move out of a blocked planet")
        this.planet = planet
    }

    fun block() {
        this.reduceEnergy(round(2 + 0.1 * maxEnergy).toInt())
        this.planet.blocked = true // TODO make sure this gets reset every round
    }

    fun receiveDamage(damage: Int) {
        this.health -= damage
        if (health <= 0) alive = false
    }

    private fun reduceEnergy(amount: Int) {
        if (amount > energy) throw NotEnoughEnergyException("Tried to reduce energy by $amount but only has $energy energy")
        if (amount < 0) throw IllegalArgumentException("Used energy amount cannot be less than zero")

        energy -= amount
    }

    fun attack(otherRobot: Robot) {
        reduceEnergy(damageLevel + 1)
        otherRobot.receiveDamage(attackDamage)
    }

    fun upgrade(upgradeType: UpgradeType) {
        when (upgradeType) {
            UpgradeType.DAMAGE -> damageLevel++
            UpgradeType.ENERGY_REGEN -> energyRegenLevel++
            UpgradeType.MAX_ENERGY -> energyLevel++
            UpgradeType.HEALTH -> healthLevel++
            UpgradeType.STORAGE -> inventory.storageLevel++
            UpgradeType.MINING_SPEED -> miningSpeedLevel++
            UpgradeType.MINING -> miningLevel++
        }
    }

    fun canMine(resourceType: ResourceType): Boolean {
        return this.miningLevel >= resourceType.requiredMiningLevel
    }

    fun totalUpgrades(): Int {
        return damageLevel + energyLevel + energyRegenLevel + healthLevel + inventory.storageLevel + miningSpeedLevel + miningLevel
    }

    /**
     * Regenerates this [Robot's] [Robot] `energy`.
     */
    fun regenerateEnergy() {
        energy += energyRegen
    }

    /**
     * Repairs this [Robot] to full health.
     */
    fun repair() {
        this.health = this.maxHealth
    }
}

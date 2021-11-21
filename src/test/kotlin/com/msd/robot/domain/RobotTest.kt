package com.msd.robot.domain

import com.msd.domain.ResourceType
import com.msd.planet.domain.Planet
import com.msd.robot.domain.exceptions.NotEnoughEnergyException
import com.msd.robot.domain.exceptions.PlanetBlockedException
import com.msd.robot.domain.exceptions.UpgradeException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.util.*

class RobotTest {

    private lateinit var robot1: Robot
    private lateinit var robot2: Robot

    @BeforeEach
    fun initializeRobots() {
        val planet = Planet(UUID.randomUUID())
        robot1 = Robot(UUID.randomUUID(), planet)
        robot2 = Robot(UUID.randomUUID(), planet)
    }

    @Test
    fun `Robot uses Energy after moving`() {
        // when
        robot1.move(Planet(UUID.randomUUID()), 3)

        // then
        assertEquals(17, robot1.energy)
    }

    @Test
    fun `Robot changes planet after successfully moving`() {
        // given
        val planet = Planet(UUID.randomUUID())
        // when
        robot1.move(planet, 3)
        // then
        assertEquals(planet, robot1.planet)
    }

    @Test
    fun `Robot does not move if it does not have enough energy`() {
        // when
        val initalPlanet = robot1.planet
        val newPlanet = Planet(UUID.randomUUID())
        // then
        assertThrows<NotEnoughEnergyException> {
            robot1.move(newPlanet, 21)
        }
        assertAll(
            {
                assertEquals(initalPlanet, robot1.planet)
            },
            {
                assertNotEquals(newPlanet, robot1.planet)
            }
        )
    }

    @Test
    fun `Robot can't move if current planet is blocked`() {
        // given
        val planet = Planet(UUID.randomUUID())
        robot1.move(planet, 0)
        robot2.move(planet, 0)
        robot2.block()
        // then
        assertThrows<PlanetBlockedException> {
            robot1.move(Planet(UUID.randomUUID()), 1)
        }
        assertEquals(planet, robot1.planet)
    }

    @Test
    fun `Robot still uses energy when trying to escape blocked planet`() {
        // given
        val planet = Planet(UUID.randomUUID())
        robot1.move(planet, 0)
        robot2.move(planet, 0)
        robot2.block()
        // then
        assertThrows<PlanetBlockedException> {
            robot1.move(Planet(UUID.randomUUID()), 1)
        }
        assertEquals(robot1.energy, robot1.maxEnergy - 1)
    }

    @Test
    fun `Robot can enter blocked planet`() {
        // given
        val planet = Planet(UUID.randomUUID())
        robot2.move(planet, 0)
        robot2.block()
        // when
        robot1.move(planet, 1)
        // then
        assertEquals(planet, robot1.planet)
    }

    @Test
    fun `Robot is dead after health reaches 0 or less`() {
        // when
        robot1.receiveDamage(12)
        robot2.receiveDamage(10)

        // assert
        assert(robot1.health <= 0)
        assertFalse(robot1.alive)
        assert(robot2.health == 0)
        assertFalse(robot1.alive)
    }

    @Test
    fun `Robot health reduced by damage received`() {
        // when
        robot1.receiveDamage(9)

        // assert
        assert(robot1.health == 1)
        assert(robot1.alive)
    }

    @Test
    fun `Robot reduces energy when attacking`() {
        // when
        robot1.attack(robot2)

        // assert
        assertEquals(robot1.maxEnergy - (robot1.damageLevel + 1), robot1.energy)
    }

    @Test
    fun `Robot causes damage to second robot according to its attackDamage when attacking`() {
        // when
        robot1.attack(robot2)

        // assert
        assert(robot2.health == UpgradeValues.maxHealthByLevel[0] - UpgradeValues.attackDamageByLevel[0])
    }

    @Test
    fun `Robot causes no damage if he doesnt have enough energy to attack`() {
        // given
        for (i in 0 until 10) robot1.move(Planet(UUID.randomUUID()), 2)

        // then
        assertThrows<NotEnoughEnergyException>("Tried to reduce energy by 1 but only has 0 energy") {
            robot1.attack(robot2)
        }
        assertEquals(10, robot2.health)
    }

    @Test
    fun `Upgrading a robot increases its upgrade level`() {
        // given
        for (upgradeType in UpgradeType.values()) robot1.upgrade(upgradeType, 1)
        // then
        assertAll(
            "All upgrade levels increase to 1",
            {
                assertEquals(1, robot1.inventory.storageLevel)
            },
            {
                assertEquals(1, robot1.healthLevel)
            },
            {
                assertEquals(1, robot1.damageLevel)
            },
            {
                assertEquals(1, robot1.miningSpeedLevel)
            },
            {
                assertEquals(1, robot1.miningLevel)
            },
            {
                assertEquals(1, robot1.energyLevel)
            },
            {
                assertEquals(1, robot1.energyRegenLevel)
            },
        )
        assertEquals(7, robot1.totalUpgrades())
    }

    @Test
    fun `Upgrading the level causes corresponding stat changes`() {
        // given
        for (upgradeType in UpgradeType.values()) robot1.upgrade(upgradeType, 1)

        // then
        assertAll(
            "Assert upgrading changes values",
            {
                assertEquals(UpgradeValues.storageByLevel[1], robot1.inventory.maxStorage)
            },
            {
                assertEquals(UpgradeValues.maxHealthByLevel[1], robot1.maxHealth)
            },
            {
                assertEquals(UpgradeValues.attackDamageByLevel[1], robot1.attackDamage)
            },
            {
                assertEquals(UpgradeValues.miningSpeedByLevel[1], robot1.miningSpeed)
            },
            {
                assertTrue(robot1.canMine(ResourceType.IRON))
            },
            {
                assertEquals(UpgradeValues.maxEnergyByLevel[1], robot1.maxEnergy)
            },
            {
                assertEquals(UpgradeValues.energyRegenByLevel[1], robot1.energyRegen)
            }
        )
    }

    @Test
    fun `Can't downgrade the level of a Robot`() {
        // given
        robot1.upgrade(UpgradeType.HEALTH, 1)
        // when
        assertThrows<UpgradeException> {
            robot1.upgrade(UpgradeType.HEALTH, 0)
        }
        // then
        assertEquals(1, robot1.healthLevel)
    }

    @Test
    fun `The upgrade level of a robot cannot go higher than 5 (except MiningLevel)`() {
        // given
        for (upgradeType in UpgradeType.values()) {
            if (upgradeType != UpgradeType.MINING)
                for (i in 1..5) robot1.upgrade(upgradeType, i)
        }
        // assert
        assertAll(
            "Assert all upgrade levels being maxed at 5",
            {
                assertThrows<UpgradeException>("Max Storage Level has been reached. Upgrade not possible.") {
                    robot1.upgrade(UpgradeType.STORAGE, 6)
                }
            },
            {
                assertThrows<UpgradeException>("Max Health Level has been reached. Upgrade not possible.") {
                    robot1.upgrade(UpgradeType.HEALTH, 6)
                }
            },
            {
                assertThrows<UpgradeException>("Max Damage Level has been reached. Upgrade not possible.") {
                    robot1.upgrade(UpgradeType.DAMAGE, 6)
                }
            },
            {
                assertThrows<UpgradeException>("Max Mining Speed has been reached. Upgrade not possible.") {
                    robot1.upgrade(UpgradeType.MINING_SPEED, 6)
                }
            },
            {
                assertThrows<UpgradeException>("Max Energy Level has been reached. Upgrade not possible.") {
                    robot1.upgrade(UpgradeType.MAX_ENERGY, 6)
                }
            },
            {
                assertThrows<UpgradeException>("Max Energy Regen has been reached. Upgrade not possible.") {
                    robot1.upgrade(UpgradeType.ENERGY_REGEN, 6)
                }
            }
        )
    }

    @Test
    fun `The Mining Upgrade Level of a robot cannot go higher than 4`() {
        for (i in 1..4) robot1.upgrade(UpgradeType.MINING, i)
        assertThrows<UpgradeException>("Max Mining Level has been reached. Upgrade not possible.") {
            robot1.upgrade(UpgradeType.MINING, 5)
        }
    }

    @Test
    fun `Energy regen increases current energy by the correct amount`() {
        // given
        robot1.move(Planet(UUID.randomUUID()), 5)
        robot2.move(Planet(UUID.randomUUID()), 7)
        // when
        robot1.regenerateEnergy()
        robot2.regenerateEnergy()
        // then
        assertAll(
            {
                assertEquals(19, robot1.energy)
            },
            {
                assertEquals(17, robot2.energy)
            }
        )
    }

    @Test
    fun `Can't regenerate Energy past max Energy amount`() {
        // given
        robot1.move(Planet(UUID.randomUUID()), 1)
        // when
        robot1.regenerateEnergy()
        // then
        assertEquals(20, robot1.energy)
    }

    @Test
    fun `Health regenerates successfully`() {
        // given
        robot1.receiveDamage(5)
        // when
        robot1.repair()
        // then
        assertEquals(robot1.maxHealth, robot1.health)
    }
}

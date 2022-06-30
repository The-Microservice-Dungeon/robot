package com.msd.robot.application.dtos

import java.util.*

class RobotDto(
    val id: UUID,
    val player: UUID,
    val planet: UUID,
    val alive: Boolean,
    val maxHealth: Int,
    val maxEnergy: Int,
    val energyRegen: Int,
    val attackDamage: Int,
    val miningSpeed: Int,
    val health: Int,
    val energy: Int,
    val healthLevel: Int,
    val damageLevel: Int,
    val miningSpeedLevel: Int,
    val miningLevel: Int,
    val energyLevel: Int,
    val energyRegenLevel: Int,
    val storageLevel: Int,
    val inventory: InventoryDto,
 //   val items: ItemsDto
)

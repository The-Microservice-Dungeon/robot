package com.msd.admin.application

import com.msd.robot.domain.gameplayVariables.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class GameplayVariableApplicationService(
    val storageLevelRepository: StorageLevelRepository,
    val healthLevelRepository: HealthLevelRepository,
    val damageLevelRepository: DamageLevelRepository,
    val miningSpeedLevelRepository: MiningSpeedLevelRepository,
    val energyCapacityLevelRepository: EnergyCapacityLevelRepository,
    val energyRegenerationLevelRepository: EnergyRegenerationLevelRepository,
    val energyCostCalculationValueRepository: EnergyCostCalculationValueRepository
) {
    fun patchGameplayVariables(patchDto: GameplayVariablesDTO): GameplayVariablesDTO {
        val changedDTO = GameplayVariablesDTO()

        if (patchDto.storage.isNotEmpty()) {
            changedDTO.storage = patchStorageVariables(patchDto.storage)
        }
        if (patchDto.hp.isNotEmpty()) {
            changedDTO.hp = patchHealthVariables(patchDto.hp)
        }
        if (patchDto.damage.isNotEmpty()) {
            changedDTO.damage = patchDamageVariables(patchDto.damage)
        }
        if (patchDto.miningSpeed.isNotEmpty()) {
            changedDTO.miningSpeed = patchMiningSpeedVariables(patchDto.miningSpeed)
        }
        if (patchDto.energyCapacity.isNotEmpty()) {
            changedDTO.energyCapacity = patchEnergyCapacityVariables(patchDto.energyCapacity)
        }
        if (patchDto.energyRegeneration.isNotEmpty()) {
            changedDTO.energyRegeneration = patchEnergyRegenerationVariables(patchDto.energyRegeneration)
        }
        if (patchDto.energyCostCalculation.isNotEmpty()) {
            changedDTO.energyCostCalculation = patchEnergyCalculationVariables(patchDto.energyCostCalculation)
        }

        return changedDTO
    }

    // TODO("application runner, commandline runner spring")

    fun patchStorageVariables(map: Map<GameplayVariablesLevelVerbs, Int>): Map<GameplayVariablesLevelVerbs, Int> {
        val storageLevel = storageLevelRepository.findByIdOrNull("STORAGE")!!

        storageLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                storageLevel.levels[it.key] = map[it.key]!!
            }
        }

        val changedLevelValues = storageLevelRepository.save(storageLevel)
        return changedLevelValues.levels
    }

    fun patchHealthVariables(map: Map<GameplayVariablesLevelVerbs, Int>): Map<GameplayVariablesLevelVerbs, Int> {
        val healthLevel = healthLevelRepository.findByIdOrNull("HEALTH")!!

        healthLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                healthLevel.levels[it.key] = map[it.key]!!
            }
        }

        val changedLevelValues = healthLevelRepository.save(healthLevel)
        return changedLevelValues.levels
    }

    fun patchDamageVariables(map: Map<GameplayVariablesLevelVerbs, Int>): Map<GameplayVariablesLevelVerbs, Int> {
        val damageLevel = damageLevelRepository.findByIdOrNull("DAMAGE")!!

        damageLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                damageLevel.levels[it.key] = map[it.key]!!
            }
        }

        val changedLevelValues = damageLevelRepository.save(damageLevel)
        return changedLevelValues.levels
    }

    fun patchMiningSpeedVariables(map: Map<GameplayVariablesLevelVerbs, Int>): Map<GameplayVariablesLevelVerbs, Int> {
        val miningSpeedLevel = miningSpeedLevelRepository.findByIdOrNull("MININGSPEED")!!

        miningSpeedLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                miningSpeedLevel.levels[it.key] = map[it.key]!!
            }
        }

        val changedLevelValues = miningSpeedLevelRepository.save(miningSpeedLevel)
        return changedLevelValues.levels
    }

    fun patchEnergyCapacityVariables(map: Map<GameplayVariablesLevelVerbs, Int>): Map<GameplayVariablesLevelVerbs, Int> {
        val energyCapacityLevel = energyCapacityLevelRepository.findByIdOrNull("ENERGYCAPACITY")!!

        energyCapacityLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                energyCapacityLevel.levels[it.key] = map[it.key]!!
            }
        }

        val changedLevelValues = energyCapacityLevelRepository.save(energyCapacityLevel)
        return changedLevelValues.levels
    }

    fun patchEnergyRegenerationVariables(map: Map<GameplayVariablesLevelVerbs, Int>): Map<GameplayVariablesLevelVerbs, Int> {
        val energyRegenerationLevel = energyRegenerationLevelRepository.findByIdOrNull("ENERGYREGENERATION")!!

        energyRegenerationLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                energyRegenerationLevel.levels[it.key] = map[it.key]!!
            }
        }

        val changedLevelValues = energyRegenerationLevelRepository.save(energyRegenerationLevel)
        return changedLevelValues.levels
    }

    fun patchEnergyCalculationVariables(map: Map<EnergyCostCalculationVerbs, Double>): Map<EnergyCostCalculationVerbs, Double> {
        val energyCostCalculationValue = energyCostCalculationValueRepository.findByIdOrNull("ENERGYCOSTCALCULATION")!!

        energyCostCalculationValue.levels.forEach {
            if (map.containsKey(it.key)) {
                energyCostCalculationValue.levels[it.key] = map[it.key]!!
            }
        }

        val changedLevelValues = energyCostCalculationValueRepository.save(energyCostCalculationValue)
        return changedLevelValues.levels
    }
}

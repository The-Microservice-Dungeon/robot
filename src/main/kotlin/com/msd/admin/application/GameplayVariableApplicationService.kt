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
    fun patchGameplayVariables(patchDto: GameplayVariablesDTO) {
        if (patchDto.storage.isNotEmpty()) {
            patchStorageVariables(patchDto.storage)
        }
        if (patchDto.hp.isNotEmpty()) {
            patchHealthVariables(patchDto.hp)
        }
        if (patchDto.damage.isNotEmpty()) {
            patchDamageVariables(patchDto.damage)
        }
        if (patchDto.miningSpeed.isNotEmpty()) {
            patchMiningSpeedVariables(patchDto.miningSpeed)
        }
        if (patchDto.energyCapacity.isNotEmpty()) {
            patchEnergyCapacityVariables(patchDto.energyCapacity)
        }
        if (patchDto.energyRegeneration.isNotEmpty()) {
            patchEnergyRegenerationVariables(patchDto.energyRegeneration)
        }
        if (patchDto.energyCostCalculation.isNotEmpty()) {
            patchEnergyCalculationVariables(patchDto.energyCostCalculation)
        }
    }

    // TODO("application runner, commandline runner spring, level enums über zahlenwerte")
    // TODO("Return changed Values")

    fun patchStorageVariables(map: Map<GameplayVariablesLevelVerbs, Int>) {
        val storageLevel = storageLevelRepository.findByIdOrNull("STORAGE")!!

        storageLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                storageLevel.levels[it.key] = map[it.key]!!
            }
        }

        storageLevelRepository.save(storageLevel)
    }

    fun patchHealthVariables(map: Map<GameplayVariablesLevelVerbs, Int>) {
        val healthLevel = healthLevelRepository.findByIdOrNull("HP")!!

        healthLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                healthLevel.levels[it.key] = map[it.key]!!
            }
        }

        healthLevelRepository.save(healthLevel)
    }

    fun patchDamageVariables(map: Map<GameplayVariablesLevelVerbs, Int>) {
        val damageLevel = damageLevelRepository.findByIdOrNull("DAMAGE")!!

        damageLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                damageLevel.levels[it.key] = map[it.key]!!
            }
        }

        damageLevelRepository.save(damageLevel)
    }

    fun patchMiningSpeedVariables(map: Map<GameplayVariablesLevelVerbs, Int>) {
        val miningSpeedLevel = miningSpeedLevelRepository.findByIdOrNull("MININGSPEED")!!

        miningSpeedLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                miningSpeedLevel.levels[it.key] = map[it.key]!!
            }
        }

        miningSpeedLevelRepository.save(miningSpeedLevel)
    }

    fun patchEnergyCapacityVariables(map: Map<GameplayVariablesLevelVerbs, Int>) {
        val energyCapacityLevel = energyCapacityLevelRepository.findByIdOrNull("ENERGYCAPACITY")!!

        energyCapacityLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                energyCapacityLevel.levels[it.key] = map[it.key]!!
            }
        }

        energyCapacityLevelRepository.save(energyCapacityLevel)
    }

    fun patchEnergyRegenerationVariables(map: Map<GameplayVariablesLevelVerbs, Int>) {
        val energyRegenerationLevel = energyRegenerationLevelRepository.findByIdOrNull("ENERGYREGENERATION")!!

        energyRegenerationLevel.levels.forEach {
            if (map.containsKey(it.key)) {
                energyRegenerationLevel.levels[it.key] = map[it.key]!!
            }
        }

        energyRegenerationLevelRepository.save(energyRegenerationLevel)
    }

    fun patchEnergyCalculationVariables(map: Map<EnergyCostCalculationVerbs, Double>) {
        val energyCostCalculationValue = energyCostCalculationValueRepository.findByIdOrNull("ENERGYCOSTCALCULATION")!!

        energyCostCalculationValue.values.forEach {
            if (map.containsKey(it.key)) {
                energyCostCalculationValue.values[it.key] = map[it.key]!!
            }
        }

        energyCostCalculationValueRepository.save(energyCostCalculationValue)
    }
}

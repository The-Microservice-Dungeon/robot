package com.msd.admin.application

import com.msd.robot.domain.gameplayVariables.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import javax.persistence.EntityNotFoundException

@Service
class GameplayVariableApplicationService(
    val upgradeValuesRepository: UpgradeValuesRepository,
    val energyCostCalculationValuesRepository: EnergyCostCalculationValuesRepository,
) {
    fun patchGameplayVariables(patchDTO: GameplayVariablesDTO): GameplayVariablesDTO {
        val changedDTO = GameplayVariablesDTO()
        val upgradeValues = patchUpgradeValues(patchDTO)

        changedDTO.hp = upgradeValues.healthValues
        changedDTO.storage = upgradeValues.storageValues
        changedDTO.miningSpeed = upgradeValues.miningSpeedValues
        changedDTO.energyCapacity = upgradeValues.energyCapacityValues
        changedDTO.energyRegeneration = upgradeValues.energyRegenerationValues
        changedDTO.damage = upgradeValues.damageValues
        changedDTO.energyCostCalculation = patchEnergyCalculationVariables(patchDTO.energyCostCalculation)

        return changedDTO
    }

    fun patchUpgradeValues(patchDTO: GameplayVariablesDTO): UpgradeValues {
        val upgradeValues = upgradeValuesRepository.findByIdOrNull("VALUES")
            ?: throw EntityNotFoundException("Upgrade Values could not be found")
        upgradeValues.damageValues.putAll(patchDTO.damage)
        upgradeValues.healthValues.putAll(patchDTO.hp)
        upgradeValues.storageValues.putAll(patchDTO.storage)
        upgradeValues.miningSpeedValues.putAll(patchDTO.miningSpeed)
        upgradeValues.energyCapacityValues.putAll(patchDTO.energyCapacity)
        upgradeValues.energyRegenerationValues.putAll(patchDTO.energyRegeneration)
        return upgradeValuesRepository.save(upgradeValues)
    }

    fun patchEnergyCalculationVariables(energyCalculationValues: Map<EnergyCostCalculationVerbs, Double>): Map<EnergyCostCalculationVerbs, Double> {
        val energyCostCalculationValue = energyCostCalculationValuesRepository.findByIdOrNull("ENERGY_COST_CALCULATION")!!
        energyCostCalculationValue.energyCostValues.putAll(energyCalculationValues)
        energyCostCalculationValuesRepository.save(energyCostCalculationValue)
        return energyCostCalculationValue.energyCostValues
    }
}

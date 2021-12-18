package com.msd.admin.domain

import com.msd.robot.domain.gameplayVariables.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GameplayVariableDomainService(
    @Autowired var storageLevelRepository: StorageLevelRepository,
    @Autowired var healthLevelRepository: HealthLevelRepository,
    @Autowired var damageLevelRepository: DamageLevelRepository,
    @Autowired var energyCapacityLevelRepository: EnergyCapacityLevelRepository,
    @Autowired var energyRegenerationLevelRepository: EnergyRegenerationLevelRepository,
    @Autowired var miningSpeedLevelRepository: MiningSpeedLevelRepository,
    @Autowired var energyCostCalculationValueRepository: EnergyCostCalculationValueRepository
) {
    fun createDefaultGameplayVariables() {
        storageLevelRepository.save(StorageLevel())
        healthLevelRepository.save(HealthLevel())
        damageLevelRepository.save(DamageLevel())
        miningSpeedLevelRepository.save(MiningSpeedLevel())
        energyCapacityLevelRepository.save(EnergyCapacityLevel())
        energyRegenerationLevelRepository.save(EnergyRegenerationLevel())
        energyCostCalculationValueRepository.save(EnergyCostCalculationValue())
    }
}

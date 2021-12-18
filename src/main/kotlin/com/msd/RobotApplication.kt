package com.msd

import com.msd.robot.domain.gameplayVariables.*
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class RobotApplication {
    @Bean
    fun init(
        storageLevelRepository: StorageLevelRepository,
        healthLevelRepository: HealthLevelRepository,
        damageLevelRepository: DamageLevelRepository,
        miningSpeedLevelRepository: MiningSpeedLevelRepository,
        energyCapacityLevelRepository: EnergyCapacityLevelRepository,
        energyRegenerationLevelRepository: EnergyRegenerationLevelRepository,
        energyCostCalculationValueRepository: EnergyCostCalculationValueRepository
    ) = CommandLineRunner {
        val storage = StorageLevel()
        var energy = EnergyCapacityLevel()

        storageLevelRepository.save(StorageLevel())
        healthLevelRepository.save(HealthLevel())
        damageLevelRepository.save(DamageLevel())
        miningSpeedLevelRepository.save(MiningSpeedLevel())
        energyCapacityLevelRepository.save(EnergyCapacityLevel())
        energyRegenerationLevelRepository.save(EnergyRegenerationLevel())
        energyCostCalculationValueRepository.save(EnergyCostCalculationValue())
    }
}

fun main(args: Array<String>) {
    runApplication<RobotApplication>(*args)
}

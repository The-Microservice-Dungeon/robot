package com.msd

import com.msd.robot.domain.gameplayVariables.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class RobotApplication {
    @Bean
    fun init(
        @Autowired upgradeValuesRepository: UpgradeValuesRepository,
        @Autowired energyCostCalculationValuesRepository: EnergyCostCalculationValuesRepository
    ) = CommandLineRunner {
        if (upgradeValuesRepository.findAll().toList().isEmpty()) {
            upgradeValuesRepository.save(UpgradeValues())
        }
        if (energyCostCalculationValuesRepository.findAll().toList().isEmpty()) {
            energyCostCalculationValuesRepository.save(EnergyCostCalculationValues())
        }
    }
}

fun main(args: Array<String>) {
    runApplication<RobotApplication>(*args)
}

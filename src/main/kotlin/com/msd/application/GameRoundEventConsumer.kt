package com.msd.application

import com.msd.planet.domain.PlanetRepository
import com.msd.robot.domain.UpgradeValues
import com.msd.robot.domain.gameplayVariables.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class GameRoundEventConsumer(
    val planetRepository: PlanetRepository,
    val storageLevelRepository: StorageLevelRepository,
    val healthLevelRepository: HealthLevelRepository,
    val damageLevelRepository: DamageLevelRepository,
    val miningSpeedLevelRepository: MiningSpeedLevelRepository,
    val energyCapacityLevelRepository: EnergyCapacityLevelRepository,
    val energyRegenerationLevelRepository: EnergyRegenerationLevelRepository,
    val energyCostCalculationValueRepository: EnergyCostCalculationValueRepository
) {

    @KafkaListener(id = "gameRoundListener", topics = ["\${spring.kafka.topic.consumer.round}"])
    fun gameRoundListener(record: ConsumerRecord<String, String>) {
        val payload = record.value() // TODO update if the payload isn't just a string
        if (payload == "ended") {
            resetBlocks()
            // patchGameplayVariables()
        }
    }

    fun resetBlocks() {
        val planets = planetRepository.findAllByBlocked(true)
        planets.forEach { it.blocked = false }
        planetRepository.saveAll(planets)
    }

    fun patchGameplayVariables() {

        StorageLevelObject.levels = storageLevelRepository.findByIdOrNull("STORAGE")!!.levels
        HealthLevelObject.levels = healthLevelRepository.findByIdOrNull("HEALTH")!!.levels
        DamageLevelObject.levels = damageLevelRepository.findByIdOrNull("DAMAGE")!!.levels
        MiningSpeedLevelObject.levels = miningSpeedLevelRepository.findByIdOrNull("MININGSPEED")!!.levels
        EnergyCapacityLevelObject.levels = energyCapacityLevelRepository.findByIdOrNull("ENERGYCAPACITY")!!.levels
        EnergyRegenerationLevelObject.levels =
            energyRegenerationLevelRepository.findByIdOrNull("ENERGYREGENERATION")!!.levels

        EnergyCostCalculationValueObject.levels =
            energyCostCalculationValueRepository.findByIdOrNull("ENERGYCOSTCALCULATION")!!.levels

        UpgradeValues.storageByLevel = StorageLevelObject.levels
        UpgradeValues.maxHealthByLevel = HealthLevelObject.levels
        UpgradeValues.attackDamageByLevel = DamageLevelObject.levels
        UpgradeValues.miningSpeedByLevel = MiningSpeedLevelObject.levels
        UpgradeValues.maxEnergyByLevel = EnergyCapacityLevelObject.levels
        UpgradeValues.energyRegenByLevel = EnergyRegenerationLevelObject.levels

        EnergyCostCalculationValueObject.levels = EnergyCostCalculationValueObject.levels
    }
}

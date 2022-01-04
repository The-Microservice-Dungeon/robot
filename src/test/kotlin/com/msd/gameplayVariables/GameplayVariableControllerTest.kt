package com.msd.gameplayVariables

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.admin.application.EnergyCostCalculationVerbs
import com.msd.admin.application.GameplayVariablesDTO
import com.msd.admin.application.GameplayVariablesLevelVerbs
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.gameplayVariables.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import java.util.*
import javax.transaction.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["no-async", " test"])
@Transactional
@DirtiesContext
class GameplayVariableControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val energyCostCalculationValuesRepository: EnergyCostCalculationValuesRepository,
    @Autowired private val upgradeValuesRepository: UpgradeValuesRepository,
    @Autowired private val robotRepository: RobotRepository
) {

    @Test
    fun `GameplayVariables get patched correctly`() {
        // given
        val patchDTO = GameplayVariablesDTO()
        patchDTO.damage = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.energyCapacity = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.energyCostCalculation = mutableMapOf(
            EnergyCostCalculationVerbs.ATTACKING_WEIGHT to 1.0,
            EnergyCostCalculationVerbs.ATTACKING_MULTIPLIER to 2.0,
            EnergyCostCalculationVerbs.MOVEMENT_MULTIPLIER to 3.0,
            EnergyCostCalculationVerbs.MINING_MULTIPLIER to 4.0,
            EnergyCostCalculationVerbs.MINING_WEIGHT to 5.0,
            EnergyCostCalculationVerbs.BLOCKING_MAX_ENERGY_PROPORTION to 6.0,
            EnergyCostCalculationVerbs.BLOCKING_BASE_COST to 7.0
        )

        patchDTO.energyRegeneration = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.hp = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.miningSpeed = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        patchDTO.storage = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
            GameplayVariablesLevelVerbs.LVL1 to 1,
            GameplayVariablesLevelVerbs.LVL2 to 2,
            GameplayVariablesLevelVerbs.LVL3 to 3,
            GameplayVariablesLevelVerbs.LVL4 to 4,
            GameplayVariablesLevelVerbs.LVL5 to 5
        )

        // when
        mockMvc.patch("/gameplay-variables") {
            contentType = MediaType.APPLICATION_JSON
            content = jacksonObjectMapper().writeValueAsString(patchDTO)
        }.andExpect {
            status { isOk() }
        }

        // then
        val upgradeValues = upgradeValuesRepository.findByIdOrNull("VALUES")!!

        assertAll(
            "Assert upgrade Values correct",
            {
                assertEquals(0, upgradeValues.damageValues[GameplayVariablesLevelVerbs.LVL0])
                assertEquals(1, upgradeValues.damageValues[GameplayVariablesLevelVerbs.LVL1])
                assertEquals(2, upgradeValues.damageValues[GameplayVariablesLevelVerbs.LVL2])
                assertEquals(3, upgradeValues.damageValues[GameplayVariablesLevelVerbs.LVL3])
                assertEquals(4, upgradeValues.damageValues[GameplayVariablesLevelVerbs.LVL4])
                assertEquals(5, upgradeValues.damageValues[GameplayVariablesLevelVerbs.LVL5])
            },
            {
                assertEquals(0, upgradeValues.energyCapacityValues[GameplayVariablesLevelVerbs.LVL0])
                assertEquals(1, upgradeValues.energyCapacityValues[GameplayVariablesLevelVerbs.LVL1])
                assertEquals(2, upgradeValues.energyCapacityValues[GameplayVariablesLevelVerbs.LVL2])
                assertEquals(3, upgradeValues.energyCapacityValues[GameplayVariablesLevelVerbs.LVL3])
                assertEquals(4, upgradeValues.energyCapacityValues[GameplayVariablesLevelVerbs.LVL4])
                assertEquals(5, upgradeValues.energyCapacityValues[GameplayVariablesLevelVerbs.LVL5])
            },
            {
                assertEquals(0, upgradeValues.energyRegenerationValues[GameplayVariablesLevelVerbs.LVL0])
                assertEquals(1, upgradeValues.energyRegenerationValues[GameplayVariablesLevelVerbs.LVL1])
                assertEquals(2, upgradeValues.energyRegenerationValues[GameplayVariablesLevelVerbs.LVL2])
                assertEquals(3, upgradeValues.energyRegenerationValues[GameplayVariablesLevelVerbs.LVL3])
                assertEquals(4, upgradeValues.energyRegenerationValues[GameplayVariablesLevelVerbs.LVL4])
                assertEquals(5, upgradeValues.energyRegenerationValues[GameplayVariablesLevelVerbs.LVL5])
            },
            {
                assertEquals(0, upgradeValues.miningSpeedValues[GameplayVariablesLevelVerbs.LVL0])
                assertEquals(1, upgradeValues.miningSpeedValues[GameplayVariablesLevelVerbs.LVL1])
                assertEquals(2, upgradeValues.miningSpeedValues[GameplayVariablesLevelVerbs.LVL2])
                assertEquals(3, upgradeValues.miningSpeedValues[GameplayVariablesLevelVerbs.LVL3])
                assertEquals(4, upgradeValues.miningSpeedValues[GameplayVariablesLevelVerbs.LVL4])
                assertEquals(5, upgradeValues.miningSpeedValues[GameplayVariablesLevelVerbs.LVL5])
            },
            {
                assertEquals(0, upgradeValues.healthValues[GameplayVariablesLevelVerbs.LVL0])
                assertEquals(1, upgradeValues.healthValues[GameplayVariablesLevelVerbs.LVL1])
                assertEquals(2, upgradeValues.healthValues[GameplayVariablesLevelVerbs.LVL2])
                assertEquals(3, upgradeValues.healthValues[GameplayVariablesLevelVerbs.LVL3])
                assertEquals(4, upgradeValues.healthValues[GameplayVariablesLevelVerbs.LVL4])
                assertEquals(5, upgradeValues.healthValues[GameplayVariablesLevelVerbs.LVL5])
            },
            {
                assertEquals(0, upgradeValues.storageValues[GameplayVariablesLevelVerbs.LVL0])
                assertEquals(1, upgradeValues.storageValues[GameplayVariablesLevelVerbs.LVL1])
                assertEquals(2, upgradeValues.storageValues[GameplayVariablesLevelVerbs.LVL2])
                assertEquals(3, upgradeValues.storageValues[GameplayVariablesLevelVerbs.LVL3])
                assertEquals(4, upgradeValues.storageValues[GameplayVariablesLevelVerbs.LVL4])
                assertEquals(5, upgradeValues.storageValues[GameplayVariablesLevelVerbs.LVL5])
            },
        )

        val energyCostValues = energyCostCalculationValuesRepository.findByIdOrNull("ENERGY_COST_CALCULATION")!!
        assertAll(
            "EnergyCostValues correct",
            {
                assertEquals(1.0, energyCostValues.energyCostValues[EnergyCostCalculationVerbs.ATTACKING_WEIGHT]!!)
                assertEquals(2.0, energyCostValues.energyCostValues[EnergyCostCalculationVerbs.ATTACKING_MULTIPLIER]!!)
                assertEquals(3.0, energyCostValues.energyCostValues[EnergyCostCalculationVerbs.MOVEMENT_MULTIPLIER]!!)
                assertEquals(4.0, energyCostValues.energyCostValues[EnergyCostCalculationVerbs.MINING_MULTIPLIER]!!)
                assertEquals(5.0, energyCostValues.energyCostValues[EnergyCostCalculationVerbs.MINING_WEIGHT]!!)
                assertEquals(
                    6.0,
                    energyCostValues.energyCostValues[EnergyCostCalculationVerbs.BLOCKING_MAX_ENERGY_PROPORTION]!!
                )
                assertEquals(7.0, energyCostValues.energyCostValues[EnergyCostCalculationVerbs.BLOCKING_BASE_COST]!!)
            }
        )
    }

    @Test
    fun `assert robot values change after upgrade and energy cost values change`() {
        // given
        val upgradeValues = upgradeValuesRepository.findByIdOrNull("VALUES")!!
        val energyCostCalculationValues =
            energyCostCalculationValuesRepository.findByIdOrNull("ENERGY_COST_CALCULATION")!!

        val robot = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()), upgradeValues, energyCostCalculationValues)
        robotRepository.save(robot)

        val patchDTO = GameplayVariablesDTO()

        patchDTO.miningSpeed = mutableMapOf(
            GameplayVariablesLevelVerbs.LVL0 to 0,
        )

        // when
        mockMvc.patch("/gameplay-variables") {
            contentType = MediaType.APPLICATION_JSON
            content = jacksonObjectMapper().writeValueAsString(patchDTO)
        }.andExpect {
            status { isOk() }
        }
        // then
        val repoRobot = robotRepository.findByIdOrNull(robot.id)!!
        assertEquals(0, repoRobot.upgradeValues.miningSpeedValues[GameplayVariablesLevelVerbs.LVL0])
        assertEquals(5, repoRobot.upgradeValues.miningSpeedValues[GameplayVariablesLevelVerbs.LVL1])
    }
}

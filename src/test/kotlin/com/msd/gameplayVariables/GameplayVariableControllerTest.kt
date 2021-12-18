package com.msd.robot.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.admin.application.EnergyCostCalculationVerbs
import com.msd.admin.application.GameplayVariablesDTO
import com.msd.admin.application.GameplayVariablesLevelVerbs
import com.msd.robot.domain.gameplayVariables.*
import com.msd.robot.domain.getByVal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["no-async", " test"])
class GameplayVariableControllerTest(
    @Autowired private var mockMvc: MockMvc,
    @Autowired private var damageLevelRepository: DamageLevelRepository,
    @Autowired private var energyCapacityLevelRepository: EnergyCapacityLevelRepository,
    @Autowired private var energyCostCalculationValueRepository: EnergyCostCalculationValueRepository,
    @Autowired private var energyRegenerationLevelRepository: EnergyRegenerationLevelRepository,
    @Autowired private var healthLevelRepository: HealthLevelRepository,
    @Autowired private var miningSpeedLevelRepository: MiningSpeedLevelRepository,
    @Autowired private var storageLevelRepository: StorageLevelRepository,
    @Autowired private var mapper: ObjectMapper,
) {

    val player1Id: UUID = UUID.fromString("d43608d5-2107-47a0-bd4f-6720dfa53c4d")

    val planet1Id: UUID = UUID.fromString("8f3c39b1-c439-4646-b646-ace4839d8849")

    @Test
    fun `GameplayVariables get patched correctly`() {
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
            EnergyCostCalculationVerbs.ATTACKINGWEIGHT to 1.0,
            EnergyCostCalculationVerbs.ATTACKINGMULTIPLIER to 1.0,
            EnergyCostCalculationVerbs.MOVEMENTMULTIPLIER to 1.0,
            EnergyCostCalculationVerbs.MININGMULTIPLIER to 1.0,
            EnergyCostCalculationVerbs.MINGINGWEIGHT to 1.0,
            EnergyCostCalculationVerbs.BLOCKINGMAXENERGYPROPORTION to 1.0,
            EnergyCostCalculationVerbs.BLOCKINGBASECOST to 1.0
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

        mockMvc.patch("/gameplay-variables") {
            contentType = MediaType.APPLICATION_JSON
            content = jacksonObjectMapper().writeValueAsString(patchDTO)
        }.andExpect {
            status { isOk() }
        }

        assertAll(
            { assert(damageLevelRepository.findByIdOrNull("DAMAGE")!!.levels.getByVal(2) == 2) },
            { assert(energyCapacityLevelRepository.findByIdOrNull("ENERGYCAPACITY")!!.levels.getByVal(2) == 2) },
            { assert(energyRegenerationLevelRepository.findByIdOrNull("ENERGYREGENERATION")!!.levels.getByVal(2) == 2) },
            { assert(healthLevelRepository.findByIdOrNull("HEALTH")!!.levels.getByVal(2) == 2) },
            { assert(miningSpeedLevelRepository.findByIdOrNull("MININGSPEED")!!.levels.getByVal(2) == 2) },
            { assert(storageLevelRepository.findByIdOrNull("STORAGE")!!.levels.getByVal(2) == 2) },

            { assert(energyCostCalculationValueRepository.findByIdOrNull("ENERGYCOSTCALCULATION")!!.levels[EnergyCostCalculationVerbs.BLOCKINGBASECOST] == 1.0) }
        )
    }
}

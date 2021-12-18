package com.msd.gameplayVariables

import com.msd.admin.application.GameplayVariableApplicationService
import com.msd.application.GameMapService
import com.msd.event.application.EventSender
import com.msd.planet.application.PlanetMapper
import com.msd.planet.domain.Planet
import com.msd.robot.application.RobotApplicationService
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotDomainService
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.gameplayVariables.*
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ExtendWith(MockKExtension::class)
@SpringBootTest
@ActiveProfiles(profiles = ["test"])
class GameplayVariablesApplicationServiceTest(
    @Autowired val planetMapper: PlanetMapper
) {
    @MockK
    lateinit var damageLevelRepository: DamageLevelRepository

    @MockK
    lateinit var energyCapacityLevelRepository: EnergyCapacityLevelRepository

    @MockK
    lateinit var energyRegenerationLevelRepository: EnergyRegenerationLevelRepository

    @MockK
    lateinit var healthLevelRepository: HealthLevelRepository

    @MockK
    lateinit var miningSpeedLevelRepository: MiningSpeedLevelRepository

    @MockK
    lateinit var storageLevelRepository: StorageLevelRepository

    @MockK
    lateinit var energyCostCalculationValueRepository: EnergyCostCalculationValueRepository

    @MockK
    lateinit var gameMapMockService: GameMapService

    @MockK
    lateinit var robotRepository: RobotRepository
    lateinit var robotApplicationService: RobotApplicationService
    lateinit var robotDomainService: RobotDomainService

    lateinit var gameplayVariablesApplicationService: GameplayVariableApplicationService

    lateinit var robot: Robot
    lateinit var planet: Planet
    private val playerId: UUID = UUID.randomUUID()

    @MockK
    lateinit var eventSender: EventSender

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        robotDomainService = RobotDomainService(robotRepository, gameMapMockService)
        robotApplicationService =
            RobotApplicationService(gameMapMockService, robotDomainService, eventSender, planetMapper)

        planet = Planet(UUID.randomUUID())
        robot = Robot(playerId, planet)

        every { robotRepository.findByIdOrNull(robot.id) } returns robot
        every { robotRepository.save(any()) } returns robot // we don't use the return value of save calls
    }
}

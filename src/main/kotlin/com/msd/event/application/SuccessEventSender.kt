package com.msd.event.application

import com.msd.application.GameMapService
import com.msd.application.dto.GameMapPlanetDto
import com.msd.command.application.command.*
import com.msd.domain.ResourceType
import com.msd.event.application.dto.*
import com.msd.planet.application.PlanetMapper
import com.msd.planet.domain.PlanetType
import com.msd.robot.application.ValidMineCommand
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotDomainService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.*

@Service
class SuccessEventSender(
    val eventSender: EventSender,
    val planetMapper: PlanetMapper,
    val robotDomainService: RobotDomainService,
    val gameMapService: GameMapService
) {

    private val logger = KotlinLogging.logger {}

    fun sendMovementItemEvents(robotPlanetPairs: MutableMap<MovementItemsUsageCommand, Pair<Robot, GameMapPlanetDto>>) {
        robotPlanetPairs.forEach { (command, pair) ->
            logger.info("[${command.transactionUUID}] Successfully executed MovementItemUsageCommand")
            val moveEventId = sendMovementEvents(pair.first, pair.second.movementDifficulty, command, pair.second)
            eventSender.sendEvent(
                ItemMovementEventDTO(
                    true,
                    "Item usage successful",
                    moveEventId
                ),
                EventType.ITEM_MOVEMENT,
                command.transactionUUID
            )
        }
    }

    /**
     * Sends the events due after a successful movement command execution
     *
     * @param robot: The robot that moved
     * @param cost: The energy costs of the movement
     * @param moveCommand: The move command that was executed
     * @param planetDto: The planet to which the robot moved, as returned from the Map Service
     * @return the event UUID of the sent movementEvent
     */
    fun sendMovementEvents(
        robot: Robot,
        cost: Int,
        moveCommand: Command,
        planetDto: GameMapPlanetDto
    ): UUID {
        logger.info("[${moveCommand.transactionUUID}] Successfully executed AttackItemUsageCommand")
        val id = eventSender.sendEvent(
            MovementEventDTO(
                true,
                "Movement successful",
                robot.energy,
                planetMapper.planetToPlanetDTO(
                    robot.planet, cost,
                    if (planetDto.spacestation) PlanetType.SPACE_STATION else PlanetType.DEFAULT
                ),
                robotDomainService.getRobotsOnPlanet(robot.planet.planetId).map { it.id }
            ),
            EventType.MOVEMENT,
            moveCommand.transactionUUID
        )
        eventSender.sendEvent(
            NeighboursEventDTO(
                planetDto.neighbours.map {
                    NeighboursEventDTO.NeighbourDTO(it.planetId, it.movementDifficulty, it.direction)
                }
            ),
            EventType.NEIGHBOURS,
            moveCommand.transactionUUID
        )
        return id
    }

    /**
     * Send all necessary events after the execution of an AttackItemUsageCommand.
     */
    fun sendAttackItemEvents(
        targetRobots: List<Robot>,
        fightingItemUsageCommand: FightingItemUsageCommand,
        robot: Robot
    ) {
        logger.info("[${fightingItemUsageCommand.transactionUUID}] Successfully executed AttackItemUsageCommand")
        val causedFightingEvents = targetRobots.map { targetRobot ->
            eventSender.sendEvent(
                FightingEventDTO(
                    true,
                    "Attacking successful",
                    fightingItemUsageCommand.robotUUID,
                    targetRobot.id,
                    targetRobot.health,
                    robot.energy
                ),
                EventType.FIGHTING,
                fightingItemUsageCommand.transactionUUID
            )
        }
        eventSender.sendEvent(
            ItemFightingEventDTO(
                true,
                "Item usage successful",
                robot.inventory.getItemAmountByType(fightingItemUsageCommand.itemType),
                causedFightingEvents
            ),
            EventType.ITEM_FIGHTING,
            fightingItemUsageCommand.transactionUUID
        )
    }

    fun sendMiningEvent(mindCommand: ValidMineCommand) {
        logger.debug("[${mindCommand.transactionId}] Sending event for successful mining")
        eventSender.sendEvent(
            MiningEventDTO(
                true,
                "Robot ${mindCommand.robot.id} mined successfully",
                mindCommand.robot.energy,
                mindCommand.robot.inventory.getStorageUsageForResource(mindCommand.resource),
                mindCommand.resource.toString()
            ),
            EventType.MINING,
            mindCommand.transactionId
        )
    }

    fun sendRepairItemEvent(
        command: RepairItemUsageCommand,
        robots: List<RepairEventRobotDTO>
    ) {
        logger.info("[${command.transactionUUID}] Successfully executed RepairItemUsageCommand")
        eventSender.sendEvent(
            ItemRepairEventDTO(
                true,
                "Robot has used ${command.itemType}",
                robots
            ),
            EventType.ITEM_REPAIR,
            command.transactionUUID
        )
    }

    fun sendFightingEvent(
        fightingCommand: FightingCommand,
        target: Robot,
        attacker: Robot
    ) {
        logger.info("[${fightingCommand.transactionUUID}] Successfully executed FightingCommand")
        eventSender.sendEvent(
            FightingEventDTO(
                true,
                "Attacking successful",
                fightingCommand.robotUUID,
                fightingCommand.targetRobotUUID,
                target.health,
                attacker.energy
            ),
            EventType.FIGHTING,
            fightingCommand.transactionUUID
        )
    }

    fun sendEnergyRegenEvent(
        robot: Robot,
        energyRegenCommand: EnergyRegenCommand
    ) {
        logger.info("[${energyRegenCommand.transactionUUID}] Successfully executed EnergyRegenCommand")
        eventSender.sendEvent(
            RegenerationEventDTO(
                true,
                "Robot regenerated ${robot.energyRegen} energy",
                robot.energy
            ),
            EventType.REGENERATION,
            energyRegenCommand.transactionUUID
        )
    }

    fun sendBlockEvent(robot: Robot, blockCommand: BlockCommand) {
        logger.info("[${blockCommand.transactionUUID}] Successfully executed BlockCommand")
        eventSender.sendEvent(
            BlockEventDTO(
                true,
                "Planet with ID: ${robot.planet.planetId} has been blocked",
                robot.planet.planetId,
                robot.energy
            ),
            EventType.PLANET_BLOCKED,
            blockCommand.transactionUUID
        )
    }

    fun sendResourceDistributionEvent(robot: Robot) {
        logger.info("Distributed resources to robot ${robot.id}")
        eventSender.sendGenericEvent(
            ResourceDistributionEventDTO(
                robot.id,
                robot.inventory.getStorageUsageForResource(ResourceType.COAL),
                robot.inventory.getStorageUsageForResource(ResourceType.IRON),
                robot.inventory.getStorageUsageForResource(ResourceType.GEM),
                robot.inventory.getStorageUsageForResource(ResourceType.GOLD),
                robot.inventory.getStorageUsageForResource(ResourceType.PLATIN),
            ),
            EventType.RESOURCE_DISTRIBUTION
        )
    }

    fun sendSpawnEvents(player: UUID, robot: Robot, transactionId: UUID) {
        logger.info("Spawned robot with ID ${robot.id} on planet ${robot.planet}")
        eventSender.sendEvent(
            SpawnEventDTO(robot.id, player, robotDomainService.getRobotsOnPlanet(robot.planet.planetId).map { it.id }.minus(robot.id)),
            EventType.ROBOT_SPAWNED,
            transactionId
        )
        val planetDto = gameMapService.getPlanet(robot.planet.planetId)
        eventSender.sendEvent(
            NeighboursEventDTO(
                planetDto.neighbours.map {
                    NeighboursEventDTO.NeighbourDTO(it.planetId, it.movementDifficulty, it.direction)
                }
            ),
            EventType.NEIGHBOURS,
            transactionId
        )
    }
}

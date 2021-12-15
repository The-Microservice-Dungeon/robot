package com.msd.robot.application

import com.msd.application.dto.GameMapPlanetDto
import com.msd.command.application.command.*
import com.msd.domain.ResourceType
import com.msd.event.application.EventSender
import com.msd.event.application.EventType
import com.msd.event.application.dto.*
import com.msd.planet.application.PlanetMapper
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotDomainService
import org.springframework.stereotype.Service
import java.util.*

@Service
class SuccessEventSender(
    val eventSender: EventSender,
    val planetMapper: PlanetMapper,
    val robotDomainService: RobotDomainService
) {

    fun sendMovementItemEvents(robotPlanetPairs: MutableMap<MovementItemsUsageCommand, Pair<Robot, GameMapPlanetDto>>) {
        robotPlanetPairs.forEach { (command, pair) ->
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
        val id = eventSender.sendEvent(
            MovementEventDTO(
                true,
                "Movement successful",
                robot.energy,
                planetMapper.planetToPlanetDTO(robot.planet, cost, planetDto.planetType), // TODO planet type?
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
        it: FightingItemUsageCommand,
        robot: Robot
    ) {
        val causedFightingEvents = targetRobots.map { targetRobot ->
            eventSender.sendEvent(
                FightingEventDTO(
                    true,
                    "Attacking successful",
                    it.robotUUID,
                    targetRobot.id,
                    targetRobot.health,
                    robot.energy
                ),
                EventType.FIGHTING,
                it.transactionUUID
            )
        }
        eventSender.sendEvent(
            ItemFightingEventDTO(
                true,
                "Item usage successful",
                robot.inventory.getItemAmountByType(it.itemType),
                causedFightingEvents
            ),
            EventType.ITEM_FIGHTING,
            it.transactionUUID
        )
    }

    fun sendMiningEvent(it: ValidMineCommand) {
        eventSender.sendEvent(
            MiningEventDTO(
                true,
                "Robot ${it.robot.id} mined successfully",
                it.robot.energy,
                it.robot.inventory.getStorageUsageForResource(it.resource),
                it.resource.toString()
            ),
            EventType.MINING,
            it.transactionId
        )
    }

    fun sendRepairItemEvent(
        command: RepairItemUsageCommand,
        robots: List<RepairEventRobotDTO>
    ) {
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
        it: FightingCommand,
        target: Robot,
        attacker: Robot
    ) {
        eventSender.sendEvent(
            FightingEventDTO(
                true,
                "Attacking successful",
                it.robotUUID,
                it.targetRobotUUID,
                target.health,
                attacker.energy
            ),
            EventType.FIGHTING,
            it.transactionUUID
        )
    }

    fun sendEnergyRegenEvent(
        robot: Robot,
        energyRegenCommand: EnergyRegenCommand
    ) {
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

    fun sendResourceDistributionEvent(it: Robot) {
        eventSender.sendGenericEvent(
            ResourceDistributionEventDTO(
                it.id,
                it.inventory.getStorageUsageForResource(ResourceType.COAL),
                it.inventory.getStorageUsageForResource(ResourceType.IRON),
                it.inventory.getStorageUsageForResource(ResourceType.GEM),
                it.inventory.getStorageUsageForResource(ResourceType.GOLD),
                it.inventory.getStorageUsageForResource(ResourceType.PLATIN),
            ),
            EventType.RESOURCE_DISTRIBUTION
        )
    }
}

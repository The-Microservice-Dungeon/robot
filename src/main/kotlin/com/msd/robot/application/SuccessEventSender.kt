package com.msd.robot.application

import com.msd.application.dto.GameMapPlanetDto
import com.msd.command.application.command.FightingItemUsageCommand
import com.msd.command.application.command.MovementCommand
import com.msd.event.application.EventSender
import com.msd.event.application.EventType
import com.msd.event.application.dto.FightingEventDTO
import com.msd.event.application.dto.ItemFightingEventDTO
import com.msd.event.application.dto.MovementEventDTO
import com.msd.event.application.dto.NeighboursEventDTO
import com.msd.planet.application.PlanetMapper
import com.msd.planet.domain.PlanetType
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

    /**
     * Send the event after executing a successful movement.
     */
    fun sendMovementEvent(
        robot: Robot,
        cost: Int,
        transactionUUID: UUID
    ): UUID {
        return eventSender.sendEvent(
            MovementEventDTO(
                true,
                "Movement successful",
                robot.energy,
                planetMapper.planetToPlanetDTO(robot.planet, cost, PlanetType.DEFAULT), // TODO planet type?
                robotDomainService.getRobotsOnPlanet(robot.planet.planetId).map { it.id }
            ),
            EventType.MOVEMENT,
            transactionUUID
        )
    }

    /**
     * Sends the events due after a successful movement command execution
     *
     * @param robot: The robot that moved
     * @param cost: The energy costs of the movement
     * @param moveCommand: The move command that was executed
     * @param planetDto: The planet to which the robot moved, as returned from the Map Service
     */
    fun sendMovementEvents(
        robot: Robot,
        cost: Int,
        moveCommand: MovementCommand,
        planetDto: GameMapPlanetDto
    ) {
        eventSender.sendEvent(
            MovementEventDTO(
                true,
                "Movement successful",
                robot.energy,
                planetMapper.planetToPlanetDTO(robot.planet, cost, PlanetType.DEFAULT), // TODO planet type?
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
}

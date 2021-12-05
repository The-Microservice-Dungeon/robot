package com.msd.event.application

import com.msd.command.application.command.*
import com.msd.core.FailureException
import com.msd.domain.DomainEvent
import com.msd.event.application.dto.*
import com.msd.planet.domain.PlanetRepository
import com.msd.robot.domain.RobotRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class EventSender(
    private val kafkaMessageProducer: KafkaMessageProducer,
    private val robotRepository: RobotRepository,
    private val topicConfig: ProducerTopicConfiguration,
    private val planetRepository: PlanetRepository
) {

    private val eventVersion = 1

    /**
     * Convert the Exception into a corresponding Kafka Event
     */
    fun handleException(fe: FailureException, command: Command) {
        when (val event = getFailureEventFromCommandAndException(command, fe)) {
            is MovementEventDTO -> {
                kafkaMessageProducer.send(
                    topicConfig.ROBOT_MOVEMENT,
                    buildDomainEvent(
                        event, EventType.MOVEMENT, command.transactionUUID
                    )
                )
            }
            is BlockEventDTO -> {
                kafkaMessageProducer.send(
                    topicConfig.ROBOT_BLOCKED,
                    buildDomainEvent(
                        event, EventType.PLANET_BLOCKED, command.transactionUUID
                    )
                )
            }
            is EnergyRegenEventDTO -> {
                kafkaMessageProducer.send(
                    topicConfig.ROBOT_REGENERATION,
                    buildDomainEvent(
                        event, EventType.REGENERATION, command.transactionUUID
                    )
                )
            }
            is MiningEventDTO -> {
                kafkaMessageProducer.send(
                    topicConfig.ROBOT_MINING,
                    buildDomainEvent(
                        event, EventType.MINING, command.transactionUUID
                    )
                )
            }
            is FightingEventDTO -> {
                kafkaMessageProducer.send(
                    topicConfig.ROBOT_FIGHTING,
                    buildDomainEvent(
                        event, EventType.FIGHTING, command.transactionUUID
                    )
                )
            }
            is ItemFightingEventDTO -> {
                kafkaMessageProducer.send(
                    topicConfig.ROBOT_ITEM_FIGHTING,
                    buildDomainEvent(
                        event, EventType.ITEM_FIGHTING, command.transactionUUID
                    )
                )
            }
            is ItemMovementEventDTO -> {
                kafkaMessageProducer.send(
                    topicConfig.ROBOT_ITEM_MOVEMENT,
                    buildDomainEvent(
                        event, EventType.ITEM_MOVEMENT, command.transactionUUID
                    )
                )
            }
            is ItemRepairEventDTO -> {
                kafkaMessageProducer.send(
                    topicConfig.ROBOT_ITEM_REPAIR,
                    buildDomainEvent(
                        event, EventType.ITEM_REPAIR, command.transactionUUID
                    )
                )
            }
        }
    }

    fun handleAll(exception: FailureException, commands: List<Command>) {
        commands.forEach {
            handleException(exception, it)
        }
    }

    /**
     * Send the Kafka DomainEvent with the given transactionId and put the given GenericEventDTO in
     */
    fun sendEvent(event: GenericEventDTO, eventType: EventType, transactionId: UUID): UUID {
        val domainEvent = buildDomainEvent(event, eventType, transactionId)
        kafkaMessageProducer.send(
            getTopicByEvent(event),
            domainEvent
        )
        return UUID.fromString(domainEvent.id)
    }

    fun sendGenericEvent(event: GenericEventDTO, eventType: EventType) {
        kafkaMessageProducer.send(
            getTopicByEvent(event),
            buildDomainEvent(event, eventType, UUID.fromString("00000000-0000-0000-0000-000000000000"))
        )
    }

    private fun buildDomainEvent(
        eventDTO: GenericEventDTO,
        eventType: EventType,
        transactionId: UUID
    ): DomainEvent<Any> {
        return DomainEvent(
            eventDTO,
            eventType.eventString,
            transactionId.toString(),
            eventVersion,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )
    }

    // TODO do we need this method? It only gets used when sending a successful event, we now what topic we need there
    private fun getTopicByEvent(event: GenericEventDTO): String {
        return when (event) {
            is MiningEventDTO -> topicConfig.ROBOT_MINING
            is FightingEventDTO -> topicConfig.ROBOT_FIGHTING
            is ResourceDistributionEventDTO -> topicConfig.ROBOT_RESOURCE_DISTRIBUTION
            is ItemFightingEventDTO -> topicConfig.ROBOT_ITEM_FIGHTING
            is MovementEventDTO -> topicConfig.ROBOT_MOVEMENT
            is NeighboursEventDTO -> topicConfig.ROBOT_NEIGHBOURS
            else -> TODO()
        }
    }

    private fun getFailureEventFromCommandAndException(command: Command, e: FailureException): EventDTO {
        val robot = robotRepository.findByIdOrNull(command.robotUUID)
        return when (command) {
            is MovementCommand -> MovementEventDTO(
                false,
                e.message!!,
                robot?.energy,
                null,
                listOf()
            )
            is BlockCommand -> BlockEventDTO(
                false,
                e.message!!,
                null,
                robot?.energy
            )
            is EnergyRegenCommand -> EnergyRegenEventDTO(
                false, e.message!!, robot?.energy
            )
            is MiningCommand -> {
                val planetResource = if (robot != null)
                    planetRepository.findByIdOrNull(robot.planet.planetId)?.resourceType?.toString() ?: "NONE"
                else
                    "NONE"
                MiningEventDTO(
                    false, e.message!!, robot?.energy, 0, planetResource
                )
            }
            is FightingCommand -> {
                val target = robotRepository.findByIdOrNull(command.targetRobotUUID)
                FightingEventDTO(
                    false,
                    e.message!!,
                    robot?.id,
                    target?.id,
                    target?.health,
                    robot?.energy
                )
            }
            is FightingItemUsageCommand -> {
                ItemFightingEventDTO(
                    false,
                    e.message!!,
                    robot?.inventory?.getItemAmountByType(command.itemType),
                    listOf()
                )
            }
            is RepairItemUsageCommand -> ItemRepairEventDTO(
                false,
                e.message!!,
                listOf()
            )
            is MovementItemsUsageCommand -> ItemMovementEventDTO(
                false,
                e.message!!,
                null
            )
            else -> throw IllegalArgumentException("Not a proper Command")
        }
    }
}

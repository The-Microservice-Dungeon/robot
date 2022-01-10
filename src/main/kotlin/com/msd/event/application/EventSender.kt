package com.msd.event.application

import com.msd.command.application.command.*
import com.msd.core.FailureException
import com.msd.domain.DomainEvent
import com.msd.event.application.dto.*
import com.msd.planet.domain.PlanetRepository
import com.msd.robot.domain.RobotRepository
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.lang.RuntimeException
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

    private val logger = KotlinLogging.logger {}

    /**
     * Convert the FailureException caused by domain logic into a corresponding Kafka Event.
     */
    fun handleFailureException(fe: FailureException, command: Command) {
        logger.info("[${command.transactionUUID}] Handling FailureException with message:\n\t${fe.message}")

        val event = getFailureEventFromCommandAndException(command, fe)
        val (topic, type) = getTopicAndTypeByEvent(event)
        kafkaMessageProducer.send(topic, buildDomainEvent(event, type, command.transactionUUID))
    }

    /**
     * Handle a FailureException caused by domain logic with affects multiple commands by throwing a KafkaEvent for
     * each command.
     */
    fun handleAll(exception: FailureException, commands: List<Command>) {
        commands.forEach {
            handleFailureException(exception, it)
        }
    }

    /**
     * Send the Kafka DomainEvent with the given transactionId and put the given GenericEventDTO in it.
     */
    fun sendEvent(event: GenericEventDTO, eventType: EventType, transactionId: UUID): UUID {
        val domainEvent = buildDomainEvent(event, eventType, transactionId)
        kafkaMessageProducer.send(
            getTopicAndTypeByEvent(event).first,
            domainEvent
        )
        return UUID.fromString(domainEvent.id)
    }

    /**
     * Send a Kafka DomainEvent which does not directly correspond to a command an thus has no transactionId.
     */
    fun sendGenericEvent(event: GenericEventDTO, eventType: EventType) {
        kafkaMessageProducer.send(
            getTopicAndTypeByEvent(event).first,
            buildDomainEvent(event, eventType, UUID.fromString("00000000-0000-0000-0000-000000000000"))
        )
    }

    /**
     * Handle RuntimeExceptions not caused by domain logic but by erroneous software behavior or during connections
     * with other services.
     */
    fun handleRuntimeException(runtimeException: RuntimeException, commands: List<Command>) {
        val failureException = FailureException(
            "Unexpected exception occurred: " +
                (runtimeException.message ?: "Unknown Error")
        )
        handleAll(failureException, commands)
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

    private fun getTopicAndTypeByEvent(event: GenericEventDTO): Pair<String, EventType> {
        return when (event) {
            is MovementEventDTO -> topicConfig.ROBOT_MOVEMENT to EventType.MOVEMENT
            is BlockEventDTO -> topicConfig.ROBOT_BLOCKED to EventType.PLANET_BLOCKED
            is MiningEventDTO -> topicConfig.ROBOT_MINING to EventType.MINING
            is ResourceDistributionEventDTO -> topicConfig.ROBOT_RESOURCE_DISTRIBUTION to EventType.RESOURCE_DISTRIBUTION
            is RegenerationEventDTO -> topicConfig.ROBOT_REGENERATION to EventType.REGENERATION
            is FightingEventDTO -> topicConfig.ROBOT_FIGHTING to EventType.FIGHTING
            is ItemFightingEventDTO -> topicConfig.ROBOT_ITEM_FIGHTING to EventType.ITEM_FIGHTING
            is NeighboursEventDTO -> topicConfig.ROBOT_NEIGHBOURS to EventType.NEIGHBOURS
            is ItemRepairEventDTO -> topicConfig.ROBOT_ITEM_REPAIR to EventType.ITEM_REPAIR
            is ItemMovementEventDTO -> topicConfig.ROBOT_ITEM_MOVEMENT to EventType.ITEM_MOVEMENT
            is RobotDestroyedEventDTO -> topicConfig.ROBOT_DESTROYED to EventType.DESTROYED
            is SpawnEventDTO -> topicConfig.ROBOT_SPAWNED to EventType.ROBOT_SPAWNED
            else -> throw RuntimeException("Unknown GenericEventDTO")
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
            is EnergyRegenCommand -> RegenerationEventDTO(
                false, e.message!!, robot?.energy
            )
            is MineCommand -> {
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
            else -> throw IllegalArgumentException("Unknown Subclass of Command")
        }
    }
}

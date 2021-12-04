package com.msd.event.application

import com.msd.command.application.command.BlockCommand
import com.msd.command.application.command.Command
import com.msd.command.application.command.EnergyRegenCommand
import com.msd.command.application.command.MovementCommand
import com.msd.core.FailureException
import com.msd.domain.DomainEvent
import com.msd.event.application.dto.*
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.exception.RobotNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class EventSender(
    private val kafkaMessageProducer: KafkaMessageProducer,
    private val robotRepository: RobotRepository,
    private val topicConfig: ProducerTopicConfiguration
) {

    private val eventVersion = 1

    /**
     * Convert the Exception into a corresponding Kafka Event
     */
    fun handle(fe: FailureException, command: Command) {
        when (val event = getEventFromCommandAndException(command, fe)) {
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
//            is NotEnoughItemsException -> TODO()
//            is TargetRobotOutOfReachException -> TODO()
//            is UpgradeException -> TODO()
//            is TargetPlanetNotReachableException -> TODO()
        }
    }

    fun handleAll(exception: FailureException, commands: List<Command>) {
        commands.forEach {
            handle(exception, it)
        }
    }

    /**
     * Send the Kafka DomainEvent with the given transactionId and put the given GenericEventDTO in
     */
    fun sendEvent(event: GenericEventDTO, transactionId: UUID): UUID {
        val domainEvent = buildDomainEvent(event, getEventTypeByEvent(event), transactionId)
        kafkaMessageProducer.send(
            getTopicByEvent(event),
            domainEvent
        )
        return UUID.fromString(domainEvent.id)
    }

    fun sendGenericEvent(event: GenericEventDTO) {
        kafkaMessageProducer.send(
            getTopicByEvent(event),
            buildDomainEvent(event, getEventTypeByEvent(event), UUID.fromString("00000000-0000-0000-0000-000000000000"))
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

    private fun getTopicByEvent(event: GenericEventDTO): String {
        return when (event) {
            is MiningEventDTO -> topicConfig.ROBOT_MINING
            is FightingEventDTO -> topicConfig.ROBOT_FIGHTING
            is ResourceDistributionEventDTO -> topicConfig.ROBOT_RESOURCE_DISTRIBUTION
            is ItemFightingEventDTO -> topicConfig.ROBOT_ITEM_FIGHTING
            else -> TODO()
        }
    }

    private fun getEventTypeByEvent(event: GenericEventDTO): EventType {
        return when (event) {
            is MiningEventDTO -> EventType.MINING
            is FightingEventDTO -> EventType.FIGHTING
            is ResourceDistributionEventDTO -> EventType.RESOURCE_DISTRIBUTION
            is ItemFightingEventDTO -> EventType.ITEM_FIGHTING
            else -> TODO()
        }
    }

    private fun getEventFromCommandAndException(command: Command, e: FailureException): EventDTO {
        val robot = if (e is RobotNotFoundException)
            null
        else
            robotRepository.findByIdOrNull(command.robotUUID)
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
//            is MiningCommand -> EventType.MINING
//            is ReparationItemUsageCommand -> EventType.ITEM_REPAIR
//            is MovementItemsUsageCommand -> EventType.ITEM_MOVEMENT
            else -> throw IllegalArgumentException("This is not a Heterogeneous command")
        }
    }
}

package com.msd.application

import com.msd.application.dto.EventDTO
import com.msd.application.dto.MovementEventDTO
import com.msd.command.application.Command
import com.msd.command.application.MovementCommand
import com.msd.core.FailureException
import com.msd.domain.DomainEvent
import com.msd.robot.application.exception.RobotNotFoundException
import com.msd.robot.domain.RobotRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class ExceptionConverter(
    private val kafkaMessageProducer: KafkaMessageProducer,
    private val robotRepository: RobotRepository
) {
    @Value(value = "\${spring.kafka.topic.producer.robot-movement}")
    private lateinit var movementTopic: String

    private val eventVersion = 1

    /**
     * Convert the Exception into a corresponding Kafka Event
     */
    fun handle(fe: FailureException, command: Command) {
        when (val event = getEventFromCommandAndException(command, fe)) {
            is MovementEventDTO -> {
                kafkaMessageProducer.send(
                    getTopicFromEventType(event.eventType),
                    buildFailureDomainEvent(
                        event, event.eventType, command.transactionUUID
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

    private fun getEventFromCommandAndException(command: Command, e: FailureException): EventDTO {
        val robot = robotRepository.findByIdOrNull(command.robotUUID) ?: throw RobotNotFoundException("")
        return when (command) {
            is MovementCommand -> MovementEventDTO(
                false,
                e.message!!,
                EventType.MOVEMENT,
                robot.energy,
                null,
                listOf()
            )
//            is BlockCommand -> Planet
//            is EnergyRegenCommand -> EventType.REGENERATION
//            is MiningCommand -> EventType.MINING
//            is ReparationItemUsageCommand -> EventType.ITEM_REPAIR
//            is MovementItemsUsageCommand -> EventType.ITEM_MOVEMENT
            else -> throw IllegalArgumentException("This is not a Heterogeneous command")
        }
    }

    private fun getTopicFromEventType(eventType: EventType): String {
        return when (eventType) {
            EventType.MOVEMENT -> movementTopic
            else -> throw IllegalArgumentException("There is not Topic for this Command")
        }
    }

    private fun buildFailureDomainEvent(
        failureEventDTO: EventDTO,
        eventType: EventType,
        transactionId: UUID
    ): DomainEvent<Any> {
        return DomainEvent(
            failureEventDTO,
            eventType.eventString,
            transactionId.toString(),
            eventVersion,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )
    }
}

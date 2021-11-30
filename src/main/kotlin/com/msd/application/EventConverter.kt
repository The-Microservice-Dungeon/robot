package com.msd.application

import com.msd.application.dto.EventDTO
import com.msd.application.dto.MovementEventDTO
import com.msd.domain.DomainEvent
import com.msd.robot.domain.exception.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class EventConverter(
    private val kafkaMessageProducer: KafkaMessageProducer
) {
    @Value(value = "\${spring.kafka.topic.producer.robot-movement}")
    private lateinit var movementTopic: String

    private val eventVersion = 1

    /**
     * Convert the Exception into a corresponding Kafka Event
     */
    fun handle(event: EventDTO, transactionId: UUID) {
        when (event) {
            is MovementEventDTO -> {
                kafkaMessageProducer.send(
                    getTopicFromEventType(event.eventType),
                    buildFailureDomainEvent(
                        event, event.eventType, transactionId
                    )
                )
            }
//            is NotEnoughItemsException -> TODO()
//            is TargetRobotOutOfReachException -> TODO()
//            is UpgradeException -> TODO()
//            is TargetPlanetNotReachableException -> TODO()
        }
    }

    // TODO replace eventType with enum
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

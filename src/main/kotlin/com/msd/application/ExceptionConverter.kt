package com.msd.application

import com.msd.application.dto.EventDTO
import com.msd.core.FailureException
import com.msd.domain.DomainEvent
import com.msd.robot.domain.exception.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class ExceptionConverter(
    private val kafkaMessageProducer: KafkaMessageProducer
) {
    @Value(value = "\${spring.kafka.topic.producer.robot-movement}")
    private lateinit var movementTopic: String

    private val eventVersion = 1

    /**
     * Convert the Exception into a corresponding Kafka Event
     */
    fun handle(exception: FailureException, transactionId: UUID) {
        // TODO add data to exceptions
        when (exception) {
//            is NotEnoughEnergyException -> TODO()
//            is NotEnoughItemsException -> TODO()
//            is NotEnoughResourcesException -> TODO()
//            is InventoryFullException -> TODO()
            is PlanetBlockedException -> {
                kafkaMessageProducer.send(
                    movementTopic,
                    buildFailureDomainEvent(
                        exception.eventDTO, "movement", transactionId
                    )
                )
            }
//            is TargetRobotOutOfReachException -> TODO()
//            is UpgradeException -> TODO()
//            is TargetPlanetNotReachableException -> TODO()
        }
    }

    // TODO replace Any with a failureDTO superclass
    private fun buildFailureDomainEvent(
        failureEventDTO: EventDTO,
        eventType: String,
        transactionId: UUID
    ): DomainEvent<Any> {
        return DomainEvent(
            failureEventDTO,
            eventType,
            transactionId.toString(),
            eventVersion,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )
    }
}

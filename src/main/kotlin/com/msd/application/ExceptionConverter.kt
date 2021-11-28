package com.msd.application

import com.msd.domain.DomainEvent
import com.msd.robot.application.exception.TargetPlanetNotReachableException
import com.msd.robot.domain.exception.*
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class ExceptionConverter(
    private val kafkaMessageProducer: KafkaMessageProducer
) {
    private val eventVersion = 1

    /**
     * Convert the Exception into a corresponding Kafka Event
     */
    fun handle(exception: RuntimeException, transactionId: UUID) {
        // TODO add data to exceptions
        when (exception) {
            is HealthFullException -> kafkaMessageProducer.send(
                "topic",
                buildFailureDomainEvent("", transactionId)
            )
            is InventoryFullException -> TODO()
            is NotEnoughEnergyException -> TODO()
            is NotEnoughItemsException -> TODO()
            is NotEnoughResourcesException -> TODO()
            is PlanetBlockedException -> TODO()
            is TargetRobotOutOfReachException -> TODO()
            is UpgradeException -> TODO()
            is TargetPlanetNotReachableException -> TODO()
        }
    }

    // TODO replace Any with a failureDTO superclass
    private fun buildFailureDomainEvent(
        failure: Any,
        transactionId: UUID
    ): DomainEvent<Any> {
        return DomainEvent(
            "",
            "failure",
            transactionId.toString(),
            eventVersion,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )
    }
}

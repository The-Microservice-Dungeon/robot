package com.msd.event.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.domain.DomainEvent
import com.msd.event.application.dto.*
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KafkaMessageProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val eventRepository: EventRepository
) {

    private val logger = KotlinLogging.logger {}

    fun send(topic: String, event: DomainEvent<*>) {
        val record = ProducerRecord(topic, event.id, jacksonObjectMapper().writeValueAsString(event.payload))
        record.headers().add("eventId", event.id.toByteArray())
        record.headers().add("transactionId", event.transactionId.toByteArray())
        record.headers().add("version", event.version.toString().toByteArray())
        record.headers().add("timestamp", event.timestamp.toByteArray())
        record.headers().add("type", event.type.toByteArray())

        val future = kafkaTemplate.send(record)

        future.addCallback(
            {
                logger.info("Message with key {${event.transactionId}} successfully send to topic: " + topic)
            },
            {
                logger.error("Failed to send message")
                val errorEvent = ErrorEvent(topic, jacksonObjectMapper().writeValueAsString(event), EventType.valueOf(event.type))
                eventRepository.save(errorEvent)
            }
        )
    }

    /**
     * Automatically resends all failed Events after a certain time. Events are saved automatically in `EventRepository` when
     * there was a failure to send them. The DomainEvent will be serialized as a String and will therefore be deserialized
     */
    @Scheduled(initialDelay = 30000L, fixedDelay = 15000)
    fun retryEvent() {
        logger.info("Trying to resend failed events...")
        val events = eventRepository.findAll()
        events.forEach {
            val event = getDomainEventFromString(it.eventString, it.eventType)
            eventRepository.delete(it)
            send(it.topic, event)
        }
    }

    /**
     * Deserializes a given [DomainEvent] from a `String` and `EventType`.
     *
     * @return a `DomainEvent` with the proper `payload`
     */
    private fun getDomainEventFromString(payloadString: String, eventType: EventType): DomainEvent<GenericEventDTO> {
        return when (eventType) {
            EventType.PLANET_BLOCKED -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, BlockEventDTO::class.java))
            EventType.MOVEMENT -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, MovementEventDTO::class.java))
            EventType.ITEM_MOVEMENT -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, ItemMovementEventDTO::class.java))
            EventType.ITEM_REPAIR -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, ItemRepairEventDTO::class.java))
            EventType.REGENERATION -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, RegenerationEventDTO::class.java))
            EventType.MINING -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, MiningEventDTO::class.java))
            EventType.ITEM_FIGHTING -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, ItemFightingEventDTO::class.java))
            EventType.FIGHTING -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, FightingEventDTO::class.java))
            EventType.RESOURCE_DISTRIBUTION -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, ResourceDistributionEventDTO::class.java))
            EventType.NEIGHBOURS -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, NeighboursEventDTO::class.java))
            EventType.DESTROYED -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, RobotDestroyedEventDTO::class.java))
            EventType.ROBOT_SPAWNED -> jacksonObjectMapper().readValue(payloadString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, SpawnEventDTO::class.java))
        }
    }
}

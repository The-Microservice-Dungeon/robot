package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.domain.DomainEvent
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaMessageProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {

    private val logger = KotlinLogging.logger {}

    fun send(topic: String, event: DomainEvent<*>) {
        val record = ProducerRecord(topic, event.id, jacksonObjectMapper().writeValueAsString(event.payload))
        record.headers().add("eventId", event.id.toByteArray())
        record.headers().add("transactionId", event.key.toByteArray())
        record.headers().add("version", event.version.toString().toByteArray())
        record.headers().add("timestamp", event.timestamp.toByteArray())
        record.headers().add("type", event.id.toByteArray())

        val future = kafkaTemplate.send(record)

        future.addCallback(
            {
                logger.info("Message with key {${event.key}} send successfully: ")
            },
            {
                logger.error("Failed to send message")
                // TODO add event send reattempt
            }
        )
    }
}

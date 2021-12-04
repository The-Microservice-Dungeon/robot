package com.msd.testUtil

import com.msd.application.EventType
import com.msd.application.dto.BlockEventDTO
import com.msd.application.dto.MovementEventDTO
import com.msd.domain.DomainEvent
import com.msd.planet.application.PlanetDTO
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertAll
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.util.*
import java.util.concurrent.BlockingQueue

class EventChecker {

    fun createMessageListenerContainer(
        embeddedKafka: EmbeddedKafkaBroker,
        topic: String,
        consumerRecords: BlockingQueue<ConsumerRecord<String, String>>
    ): KafkaMessageListenerContainer<String, String> {
        val consumerProperties: Map<String, Any> = KafkaTestUtils.consumerProps(
            "sender", "false", embeddedKafka
        )

        val containerProperties = ContainerProperties(
            topic
        )

        val consumer: DefaultKafkaConsumerFactory<String, String> =
            DefaultKafkaConsumerFactory(consumerProperties, StringDeserializer(), StringDeserializer())

        val container = KafkaMessageListenerContainer(consumer, containerProperties)
        container.setupMessageListener(
            MessageListener { record: ConsumerRecord<String, String> ->
                consumerRecords.add(record)
            }
        )

        return container
    }

    fun checkHeaders(
        expectedTransactionId: UUID,
        expectedEventType: EventType,
        domainEvent: DomainEvent<*>
    ) {
        assertAll(
            "Check header correct",
            {
                assertEquals(expectedTransactionId.toString(), domainEvent.transactionId)
            },
            {
                assertEquals(expectedEventType.eventString, domainEvent.type)
            }
        )
    }

    fun checkMovementPaylod(
        expectedSuccess: Boolean,
        expectedMessage: String,
        expectedRemainingEnergy: Int?,
        expectedPlanetDTO: PlanetDTO?,
        expectedRobots: List<UUID>,
        payload: MovementEventDTO
    ) {
        assertAll(
            "assert movement payload correct",
            {
                assertEquals(expectedSuccess, payload.success)
            },
            {
                assertEquals(expectedMessage, payload.message)
            },
            {
                assertEquals(expectedRemainingEnergy, payload.remainingEnergy)
            },
            {
                assertEquals(expectedPlanetDTO, payload.planet)
            },
            {
                assertEquals(expectedRobots, payload.robots)
            }
        )
    }

    fun checkBlockPayload(
        expectedSuccess: Boolean,
        expectedMessage: String,
        expectedPlanetId: UUID?,
        expectedRemainingEnergy: Int?,
        payload: BlockEventDTO
    ) {
        assertAll(
            "assert blocking payload correct",
            {
                assertEquals(expectedSuccess, payload.success)
            },
            {
                assertEquals(expectedMessage, payload.message)
            },
            {
                assertEquals(expectedPlanetId, payload.planetId)
            },
            {
                assertEquals(expectedRemainingEnergy, payload.remainingEnergy)
            }
        )
    }
}

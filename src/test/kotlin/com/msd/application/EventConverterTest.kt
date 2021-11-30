package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.dto.MovementEventDTO
import com.msd.domain.DomainEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://\${spring.kafka.bootstrap-servers}", "port=9092"])
internal class EventConverterTest(
    @Autowired private val eventConverter: EventConverter,
    @Autowired private val embeddedKafka: EmbeddedKafkaBroker
) {

    private lateinit var consumerRecords: BlockingQueue<ConsumerRecord<String, String>>
    private lateinit var container: KafkaMessageListenerContainer<String, String>

    @BeforeEach
    fun setup() {
        consumerRecords = LinkedBlockingQueue()

        val containerProperties = ContainerProperties("movement")

        val consumerProperties: Map<String, Any> = KafkaTestUtils.consumerProps(
            "sender", "false", embeddedKafka
        )

        val consumer: DefaultKafkaConsumerFactory<String, String> =
            DefaultKafkaConsumerFactory(consumerProperties, StringDeserializer(), StringDeserializer())

        container = KafkaMessageListenerContainer(consumer, containerProperties)
        container.setupMessageListener(
            MessageListener { record: ConsumerRecord<String, String> ->
                consumerRecords.add(record)
            }
        )
        container.start()

        ContainerTestUtils.waitForAssignment(container, embeddedKafka.partitionsPerTopic)
    }

    @AfterEach
    fun tearDown() {
        container.stop()
    }

    @Test
    fun `when a MovementEvent is handled due to a Planet block an Event is thrown in the movement topic`() {
        // given
        val movementEventDTO = MovementEventDTO(
            false, "Planet blocked", EventType.MOVEMENT, 3, null, listOf()
        )
        val transactionId = UUID.randomUUID()
        // when
        eventConverter.handle(movementEventDTO, transactionId)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        val domainEvent = DomainEvent.build(jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java), singleRecord.headers())

        assertAll(
            "Check header correct",
            {
                assertEquals(transactionId.toString(), domainEvent.transactionId)
            },
            {
                assertEquals("movement", domainEvent.type)
            }
        )

        assertAll(
            "payload correct",
            {
                assertEquals(movementEventDTO.success, domainEvent.payload.success)
            },
            {
                assertEquals(movementEventDTO.message, domainEvent.payload.message)
            },
            {
                assertEquals(movementEventDTO.energyChangedBy, domainEvent.payload.energyChangedBy)
            },
            {
                assertEquals(movementEventDTO.planet, domainEvent.payload.planet)
            },
            {
                assertEquals(movementEventDTO.robots, domainEvent.payload.robots)
            }
        )
    }

    @Test
    fun `When a MovementEvent is handled due to not enough energy an event is sent to the movement topic`() {
        // given
        val movementEventDTO = MovementEventDTO(
            false, "Not enough Energy", EventType.MOVEMENT, 0, null, listOf()
        )
        val transactionId = UUID.randomUUID()
        // when
        eventConverter.handle(movementEventDTO, transactionId)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        val domainEvent = DomainEvent.build(jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java), singleRecord.headers())

        assertAll(
            "Check header correct",
            {
                assertEquals(transactionId.toString(), domainEvent.transactionId)
            },
            {
                assertEquals("movement", domainEvent.type)
            }
        )

        assertAll(
            "payload correct",
            {
                assertEquals(movementEventDTO.success, domainEvent.payload.success)
            },
            {
                assertEquals(movementEventDTO.message, domainEvent.payload.message)
            },
            {
                assertEquals(movementEventDTO.energyChangedBy, domainEvent.payload.energyChangedBy)
            },
            {
                assertEquals(movementEventDTO.planet, domainEvent.payload.planet)
            },
            {
                assertEquals(movementEventDTO.robots, domainEvent.payload.robots)
            }
        )
    }
}

package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.dto.MovementEventDTO
import com.msd.command.application.MovementCommand
import com.msd.domain.DomainEvent
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.exception.NotEnoughEnergyException
import com.msd.robot.domain.exception.PlanetBlockedException
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
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://\${spring.kafka.bootstrap-servers}", "port=9092"]
)
@Transactional
internal class ExceptionConverterTest(
    @Autowired private val exceptionConverter: ExceptionConverter,
    @Autowired private val embeddedKafka: EmbeddedKafkaBroker,
    @Autowired private val robotRepository: RobotRepository
) {
    private lateinit var robotId: UUID

    private lateinit var consumerRecords: BlockingQueue<ConsumerRecord<String, String>>
    private lateinit var container: KafkaMessageListenerContainer<String, String>

    @BeforeEach
    fun setup() {
        val robot = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robotId = robot.id
        robotRepository.save(robot)

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
        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val planetBlockedException = PlanetBlockedException("Planet is blocked")
        // when
        exceptionConverter.handle(planetBlockedException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        assertAll(
            "Check header correct",
            {
                assertEquals(movementCommand.transactionUUID.toString(), domainEvent.transactionId)
            },
            {
                assertEquals("movement", domainEvent.type)
            }
        )

        assertAll(
            "payload correct",
            {
                assertEquals(false, domainEvent.payload.success)
            },
            {
                assertEquals("Planet is blocked", domainEvent.payload.message)
            },
            {
                assertEquals(20, domainEvent.payload.remainingEnergy)
            },
            {
                assertEquals(null, domainEvent.payload.planet)
            },
            {
                assertEquals(listOf<UUID>(), domainEvent.payload.robots)
            }
        )
    }

    @Test
    fun `When a MovementEvent is handled due to not enough energy an event is sent to the movement topic`() {
        // given
        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val notEnoughEnergyException = NotEnoughEnergyException("Not enough Energy")
        // when
        exceptionConverter.handle(notEnoughEnergyException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        assertAll(
            "Check header correct",
            {
                assertEquals(movementCommand.transactionUUID.toString(), domainEvent.transactionId)
            },
            {
                assertEquals("movement", domainEvent.type)
            }
        )

        assertAll(
            "payload correct",
            {
                assertEquals(false, domainEvent.payload.success)
            },
            {
                assertEquals("Not enough Energy", domainEvent.payload.message)
            },
            {
                assertEquals(20, domainEvent.payload.remainingEnergy)
            },
            {
                assertEquals(null, domainEvent.payload.planet)
            },
            {
                assertEquals(listOf<UUID>(), domainEvent.payload.robots)
            }
        )
    }
}

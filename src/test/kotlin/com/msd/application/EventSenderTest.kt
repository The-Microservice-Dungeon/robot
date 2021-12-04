package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.dto.BlockEventDTO
import com.msd.application.dto.MovementEventDTO
import com.msd.command.application.command.BlockCommand
import com.msd.command.application.command.MovementCommand
import com.msd.domain.DomainEvent
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.exception.NotEnoughEnergyException
import com.msd.robot.domain.exception.PlanetBlockedException
import com.msd.robot.domain.exception.RobotNotFoundException
import com.msd.testUtil.EventChecker
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
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
internal class EventSenderTest(
    @Autowired private val eventSender: EventSender,
    @Autowired private val embeddedKafka: EmbeddedKafkaBroker,
    @Autowired private val robotRepository: RobotRepository
) {
    private lateinit var robotId: UUID

    private lateinit var consumerRecords: BlockingQueue<ConsumerRecord<String, String>>
    private lateinit var movementContainer: KafkaMessageListenerContainer<String, String>
    private lateinit var blockedContainer: KafkaMessageListenerContainer<String, String>

    private val eventChecker = EventChecker()

    @Value(value = "\${spring.kafka.topic.producer.robot-movement}")
    private lateinit var movementTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-blocked}")
    private lateinit var planetBlockedTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-mining}")
    private lateinit var miningTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-fighting}")
    private lateinit var fightingTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-regeneration}")
    private lateinit var regenerationTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-item-fighting}")
    private lateinit var itemFightingTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-item-repair}")
    private lateinit var itemRepairTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-item-movement}")
    private lateinit var itemMovementTopic: String

    @BeforeEach
    fun setup() {
        val robot = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robotId = robot.id
        robotRepository.save(robot)

        consumerRecords = LinkedBlockingQueue()

        movementContainer = eventChecker.createMessageListenerContainer(embeddedKafka, movementTopic, consumerRecords)
        blockedContainer = eventChecker.createMessageListenerContainer(embeddedKafka, planetBlockedTopic, consumerRecords)

        movementContainer.start()
        blockedContainer.start()

        ContainerTestUtils.waitForAssignment(blockedContainer, embeddedKafka.partitionsPerTopic)
        ContainerTestUtils.waitForAssignment(movementContainer, embeddedKafka.partitionsPerTopic)
    }

    @AfterEach
    fun tearDown() {
        movementContainer.stop()
        blockedContainer.stop()
    }

    @Test
    fun `when a PlanetBlockedException is handled when moving an Event is thrown in the 'movement' topic`() {
        // given
        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val planetBlockedException = PlanetBlockedException("Planet is blocked")
        // when
        eventSender.handle(planetBlockedException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(movementTopic, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )
        eventChecker.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventChecker.checkMovementPaylod(false, "Planet is blocked", 20, null, listOf(), domainEvent.payload)
    }

    @Test
    fun `When a NotEnoughEnergyException is handled when moving an event is sent to the 'movement' topic`() {
        // given
        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val notEnoughEnergyException = NotEnoughEnergyException("Not enough Energy")
        // when
        eventSender.handle(notEnoughEnergyException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(movementTopic, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventChecker.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventChecker.checkMovementPaylod(false, "Not enough Energy", 20, null, listOf(), domainEvent.payload)
    }

    @Test
    fun `when a RobotNotFoundException is handled due to Movement an event is send to the 'movement' topic`() {
        // given
        val movementCommand = MovementCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot Not Found")
        // when
        eventSender.handle(robotNotFoundException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(movementTopic, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventChecker.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)

        eventChecker.checkMovementPaylod(false, "Robot Not Found", null, null, listOf(), domainEvent.payload)
    }

    @Test
    fun `when NotEnoughEnergyException is thrown while blocking an event is send to 'planet-blocked' topic`() {
        // given
        val blockCommand = BlockCommand(robotId, UUID.randomUUID())
        val notEnoughEnergyException = NotEnoughEnergyException("Robot has not enough Energy")
        // when
        eventSender.handle(notEnoughEnergyException, blockCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(planetBlockedTopic, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), BlockEventDTO::class.java),
            singleRecord.headers()
        )

        eventChecker.checkHeaders(blockCommand.transactionUUID, EventType.PLANET_BLOCKED, domainEvent)
        eventChecker.checkBlockPayload(false, "Robot has not enough Energy", null, 20, domainEvent.payload)
    }

    @Test
    fun `when RobotNotFoundException is thrown while blocking an event is send to 'planet-blocked' topic`() {
        // given
        val blockCommand = BlockCommand(robotId, UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot not Found")
        // when
        eventSender.handle(robotNotFoundException, blockCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(planetBlockedTopic, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), BlockEventDTO::class.java),
            singleRecord.headers()
        )

        eventChecker.checkHeaders(blockCommand.transactionUUID, EventType.PLANET_BLOCKED, domainEvent)
        eventChecker.checkBlockPayload(false, "Robot not Found", null, null, domainEvent.payload)
    }
}

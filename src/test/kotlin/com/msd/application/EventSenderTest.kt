package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.command.application.command.BlockCommand
import com.msd.command.application.command.EnergyRegenCommand
import com.msd.command.application.command.MovementCommand
import com.msd.domain.DomainEvent
import com.msd.event.application.EventSender
import com.msd.event.application.EventType
import com.msd.event.application.ProducerTopicConfiguration
import com.msd.event.application.dto.BlockEventDTO
import com.msd.event.application.dto.EnergyRegenEventDTO
import com.msd.event.application.dto.MovementEventDTO
import com.msd.planet.domain.Planet
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.exception.NotEnoughEnergyException
import com.msd.robot.domain.exception.PlanetBlockedException
import com.msd.robot.domain.exception.RobotNotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import java.util.*
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
    @Autowired private val robotRepository: RobotRepository,
    @Autowired private val topicConfig: ProducerTopicConfiguration
) : RobotKafkaTest(embeddedKafka, topicConfig) {

    private lateinit var robotId: UUID

    @BeforeEach
    override fun setup() {
        super.setup()
        val robot = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robotId = robot.id
        robotRepository.save(robot)
    }

    @Test
    fun `when a PlanetBlockedException is handled when moving an Event is thrown in the 'movement' topic`() {
        // given
        startMovementContainer()

        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val planetBlockedException = PlanetBlockedException("Planet is blocked")
        // when
        eventSender.handleException(planetBlockedException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(topicConfig.ROBOT_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )
        eventChecker.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventChecker.checkMovementPaylod(false, "Planet is blocked", 20, null, listOf(), domainEvent.payload)
        movementContainer.stop()
    }

    @Test
    fun `When a NotEnoughEnergyException is handled when moving an event is sent to the 'movement' topic`() {
        // given
        startMovementContainer()

        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val notEnoughEnergyException = NotEnoughEnergyException("Not enough Energy")
        // when
        eventSender.handleException(notEnoughEnergyException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(topicConfig.ROBOT_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventChecker.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventChecker.checkMovementPaylod(false, "Not enough Energy", 20, null, listOf(), domainEvent.payload)
        movementContainer.stop()
    }

    @Test
    fun `when a RobotNotFoundException is handled due to Movement an event is send to the 'movement' topic`() {
        // given
        startMovementContainer()

        val movementCommand = MovementCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot Not Found")
        // when
        eventSender.handleException(robotNotFoundException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(topicConfig.ROBOT_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventChecker.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventChecker.checkMovementPaylod(false, "Robot Not Found", null, null, listOf(), domainEvent.payload)
        movementContainer.stop()
    }

    @Test
    fun `when NotEnoughEnergyException is thrown while blocking an event is send to 'planet-blocked' topic`() {
        // given
        startPlanetBlockedContainer()

        val blockCommand = BlockCommand(robotId, UUID.randomUUID())
        val notEnoughEnergyException = NotEnoughEnergyException("Robot has not enough Energy")
        // when
        eventSender.handleException(notEnoughEnergyException, blockCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(topicConfig.ROBOT_BLOCKED, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), BlockEventDTO::class.java),
            singleRecord.headers()
        )

        eventChecker.checkHeaders(blockCommand.transactionUUID, EventType.PLANET_BLOCKED, domainEvent)
        eventChecker.checkBlockPayload(false, "Robot has not enough Energy", null, 20, domainEvent.payload)
        blockedContainer.stop()
    }

    @Test
    fun `when RobotNotFoundException is thrown while blocking an event is send to 'planet-blocked' topic`() {
        // given
        startPlanetBlockedContainer()

        val blockCommand = BlockCommand(UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot not Found")
        // when
        eventSender.handleException(robotNotFoundException, blockCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(topicConfig.ROBOT_BLOCKED, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), BlockEventDTO::class.java),
            singleRecord.headers()
        )

        eventChecker.checkHeaders(blockCommand.transactionUUID, EventType.PLANET_BLOCKED, domainEvent)
        eventChecker.checkBlockPayload(false, "Robot not Found", null, null, domainEvent.payload)
        blockedContainer.stop()
    }

    @Test
    fun `when RobotNotFoundException is thrown when regenerating an event is send to 'regeneration' topic`() {
        // given
        startRegenerationContainer()

        val regenCommand = EnergyRegenCommand(UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot not Found")
        // when
        eventSender.handleException(robotNotFoundException, regenCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord)
        assertEquals(topicConfig.ROBOT_REGENERATION, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), EnergyRegenEventDTO::class.java),
            singleRecord.headers()
        )

        eventChecker.checkHeaders(regenCommand.transactionUUID, EventType.REGENERATION, domainEvent)
        eventChecker.checkRegenerationPayload(false, "Robot not Found", null, domainEvent.payload)
        regenerationContainer.stop()
    }
}

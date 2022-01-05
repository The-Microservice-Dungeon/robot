package com.msd.event.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.AbstractKafkaProducerTest
import com.msd.domain.DomainEvent
import com.msd.event.application.dto.BlockEventDTO
import com.msd.event.application.dto.FightingEventDTO
import com.msd.event.application.dto.MovementEventDTO
import com.msd.planet.application.PlanetDTO
import com.msd.planet.domain.PlanetType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://\${spring.kafka.bootstrap-servers}", "port=9092"]
)
@Transactional
@ActiveProfiles(profiles = ["test", "no-async"])
internal class KafkaMessageProducerTest(
    @Autowired private val embeddedKafka: EmbeddedKafkaBroker,
    @Autowired private val topicConfig: ProducerTopicConfiguration,
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val kafkaMessageProducer: KafkaMessageProducer
) : AbstractKafkaProducerTest(embeddedKafka, topicConfig) {

    @Test
    fun `when resending an Event an Event is send to the correct topic`() {
        // given
        startPlanetBlockedContainer()

        val event = DomainEvent(
            BlockEventDTO(true, "planet blocked", UUID.randomUUID(), 15),
            EventType.PLANET_BLOCKED.eventString,
            UUID.randomUUID().toString(),
            1,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )

        val errorEvent = ErrorEvent(
            topicConfig.ROBOT_BLOCKED,
            jacksonObjectMapper().writeValueAsString(event),
            EventType.PLANET_BLOCKED
        )
        eventRepository.save(errorEvent)
        // when
        kafkaMessageProducer.retryEvent()
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        Assertions.assertNotNull(singleRecord!!)
        Assertions.assertEquals(topicConfig.ROBOT_BLOCKED, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), BlockEventDTO::class.java),
            singleRecord.headers()
        )
        eventTestUtils.checkHeaders(UUID.fromString(event.transactionId), EventType.PLANET_BLOCKED, domainEvent)
        eventTestUtils.checkBlockPayload(true, "planet blocked", event.payload.planetId, 15, domainEvent.payload)
    }

    @Test
    @Disabled
    fun `when resending multiple Events the events are send to the correct topics`() {
        // given
        startFightingContainer()
        startMovementContainer()
        startPlanetBlockedContainer()
        consumerRecords.clear()

        val blockEvent = DomainEvent(
            BlockEventDTO(true, "planet blocked", UUID.randomUUID(), 15),
            EventType.PLANET_BLOCKED.eventString,
            UUID.randomUUID().toString(),
            1,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )

        val moveEvent = DomainEvent(
            MovementEventDTO(true, "moved successfully", 15, PlanetDTO(UUID.randomUUID(), 5, PlanetType.DEFAULT, null)),
            EventType.MOVEMENT.eventString,
            UUID.randomUUID().toString(),
            1,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )

        val fightEvent = DomainEvent(
            FightingEventDTO(true, "attacked successfully", UUID.randomUUID(), UUID.randomUUID(), 5, 18),
            EventType.FIGHTING.eventString,
            UUID.randomUUID().toString(),
            1,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )

        val blockErrorEvent = ErrorEvent(
            topicConfig.ROBOT_BLOCKED,
            jacksonObjectMapper().writeValueAsString(blockEvent),
            EventType.PLANET_BLOCKED
        )
        val moveErrorEvent = ErrorEvent(
            topicConfig.ROBOT_MOVEMENT,
            jacksonObjectMapper().writeValueAsString(moveEvent),
            EventType.MOVEMENT
        )
        val fightingErrorEvent = ErrorEvent(
            topicConfig.ROBOT_FIGHTING,
            jacksonObjectMapper().writeValueAsString(fightEvent),
            EventType.FIGHTING
        )
        eventRepository.save(blockErrorEvent)
        eventRepository.save(moveErrorEvent)
        eventRepository.save(fightingErrorEvent)

        // when
        kafkaMessageProducer.retryEvent()

        // then
        val domainEventBlock =
            eventTestUtils.getNextEventOfTopic<BlockEventDTO>(consumerRecords, topicConfig.ROBOT_BLOCKED)

        eventTestUtils.checkHeaders(
            UUID.fromString(blockEvent.transactionId),
            EventType.PLANET_BLOCKED,
            domainEventBlock
        )
        eventTestUtils.checkBlockPayload(
            true,
            "planet blocked",
            blockEvent.payload.planetId,
            15,
            domainEventBlock.payload
        )

        val domainEventMove =
            eventTestUtils.getNextEventOfTopic<MovementEventDTO>(consumerRecords, topicConfig.ROBOT_MOVEMENT)

        eventTestUtils.checkHeaders(UUID.fromString(moveEvent.transactionId), EventType.MOVEMENT, domainEventMove)
        eventTestUtils.checkMovementPayload(
            true,
            "moved successfully",
            15,
            domainEventMove.payload.planet,
            domainEventMove.payload.robots,
            domainEventMove.payload
        )

        val domainEventFight =
            eventTestUtils.getNextEventOfTopic<FightingEventDTO>(consumerRecords, topicConfig.ROBOT_FIGHTING)

        eventTestUtils.checkHeaders(UUID.fromString(fightEvent.transactionId), EventType.FIGHTING, domainEventFight)
        eventTestUtils.checkFightingPayload(
            true,
            "attacked successfully",
            fightEvent.payload.attacker,
            fightEvent.payload.defender,
            5,
            18,
            domainEventFight.payload
        )
    }
}

package com.msd.event.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.application.AbstractKafkaProducerTest
import com.msd.domain.DomainEvent
import com.msd.event.application.dto.BlockEventDTO
import org.junit.jupiter.api.Assertions
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
@ActiveProfiles(profiles = ["test"])
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

        val errorEvent = ErrorEvent(topicConfig.ROBOT_BLOCKED, jacksonObjectMapper().writeValueAsString(event), EventType.PLANET_BLOCKED)
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
}

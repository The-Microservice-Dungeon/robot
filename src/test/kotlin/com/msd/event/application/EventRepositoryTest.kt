package com.msd.event.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.domain.DomainEvent
import com.msd.event.application.dto.BlockEventDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@SpringBootTest
@Transactional
@ActiveProfiles(profiles = ["test"])
class EventRepositoryTest(
    @Autowired private val eventRepository: EventRepository,
    @Autowired private val topicConfig: ProducerTopicConfiguration
) {

    @Test
    fun `saving and retrieving events works`() {
        // given
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
        val repoEvent = eventRepository.findByIdOrNull(errorEvent.errorId)!!
        // then

        val deserializedEvent = jacksonObjectMapper().readValue<DomainEvent<BlockEventDTO>>(repoEvent.eventString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, BlockEventDTO::class.java))
        assertAll(
            "assert retrieved event",
            {
                assertEquals(errorEvent.errorId, repoEvent.errorId)
            },
            {
                assertEquals(errorEvent.topic, repoEvent.topic)
            },
            {
                assertEquals(true, deserializedEvent.payload.success)
            },
            {
                assertEquals("planet blocked", deserializedEvent.payload.message)
            },
            {
                assertEquals(event.payload.planetId, deserializedEvent.payload.planetId)
            },
            {
                assertEquals(event.payload.remainingEnergy, 15)
            }
        )
    }
}

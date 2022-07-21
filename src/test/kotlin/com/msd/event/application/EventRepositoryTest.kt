package com.msd.event.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.domain.DomainEvent
//import com.msd.event.application.dto.BlockEventDTO
import com.msd.event.application.dto.FightingEventDTO
import com.msd.event.application.dto.MovementEventDTO
import com.msd.planet.application.PlanetDTO
import com.msd.planet.domain.PlanetType
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

  /*  @Test
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
                assertEquals(15, deserializedEvent.payload.remainingEnergy)
            }
        )
    }
*/
    @Test
    fun `can persist and retrieve multiple events of different types`() {
        // given
      /*  val blockEvent = DomainEvent(
            BlockEventDTO(true, "planet blocked", UUID.randomUUID(), 15),
            EventType.PLANET_BLOCKED.eventString,
            UUID.randomUUID().toString(),
            1,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )
*/
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

      //  val blockErrorEvent = ErrorEvent(topicConfig.ROBOT_BLOCKED, jacksonObjectMapper().writeValueAsString(blockEvent), EventType.PLANET_BLOCKED)
        val moveErrorEvent = ErrorEvent(topicConfig.ROBOT_MOVEMENT, jacksonObjectMapper().writeValueAsString(moveEvent), EventType.MOVEMENT)
        val fightingErrorEvent = ErrorEvent(topicConfig.ROBOT_FIGHTING, jacksonObjectMapper().writeValueAsString(fightEvent), EventType.FIGHTING)
       // eventRepository.save(blockErrorEvent)
        eventRepository.save(moveErrorEvent)
        eventRepository.save(fightingErrorEvent)

        // when
     //   val blockRepoEvent = eventRepository.findByIdOrNull(blockErrorEvent.errorId)!!
        val moveRepoEvent = eventRepository.findByIdOrNull(moveErrorEvent.errorId)!!
        val fightingRepoEvent = eventRepository.findByIdOrNull(fightingErrorEvent.errorId)!!
        // then

   /*     val blockDeserializedEvent = jacksonObjectMapper().readValue<DomainEvent<BlockEventDTO>>(blockRepoEvent.eventString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, BlockEventDTO::class.java))
        assertAll(
            "assert block event correct",
            {
                assertEquals(blockErrorEvent.errorId, blockRepoEvent.errorId)
            },
            {
                assertEquals(blockErrorEvent.topic, blockRepoEvent.topic)
            },
            {
                assertEquals(true, blockDeserializedEvent.payload.success)
            },
            {
                assertEquals("planet blocked", blockDeserializedEvent.payload.message)
            },
            {
                assertEquals(blockEvent.payload.planetId, blockDeserializedEvent.payload.planetId)
            },
            {
                assertEquals(15, blockDeserializedEvent.payload.remainingEnergy)
            }
        )
*/
        val moveDeserializedEvent = jacksonObjectMapper().readValue<DomainEvent<MovementEventDTO>>(moveRepoEvent.eventString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, MovementEventDTO::class.java))
        assertAll(
            "assert block event correct",
            {
                assertEquals(moveErrorEvent.errorId, moveRepoEvent.errorId)
            },
            {
                assertEquals(moveErrorEvent.topic, moveRepoEvent.topic)
            },
            {
                assertEquals(true, moveDeserializedEvent.payload.success)
            },
            {
                assertEquals("moved successfully", moveDeserializedEvent.payload.message)
            },
            {
                assertEquals(moveEvent.payload.planet!!.planetId, moveDeserializedEvent.payload.planet!!.planetId)
            },
            {
                assertEquals(moveEvent.payload.planet!!.planetType, moveDeserializedEvent.payload.planet!!.planetType)
            },
            {
                assertEquals(moveEvent.payload.planet!!.movementDifficulty, moveDeserializedEvent.payload.planet!!.movementDifficulty)
            },
            {
                assertEquals(moveEvent.payload.planet!!.resourceType, moveDeserializedEvent.payload.planet!!.resourceType)
            },
            {
                assertEquals(15, moveDeserializedEvent.payload.remainingEnergy)
            }
        )

        val fightingDeserializedEvent = jacksonObjectMapper().readValue<DomainEvent<FightingEventDTO>>(fightingRepoEvent.eventString, jacksonObjectMapper().typeFactory.constructParametricType(DomainEvent::class.java, FightingEventDTO::class.java))
        assertAll(
       /*     "assert block event correct",
            {
                assertEquals(blockErrorEvent.errorId, blockRepoEvent.errorId)
            },
            {
                assertEquals(blockErrorEvent.topic, blockRepoEvent.topic)
            },
     */       {
                assertEquals(true, fightingDeserializedEvent.payload.success)
            },
            {
                assertEquals("attacked successfully", fightingDeserializedEvent.payload.message)
            },
            {
                assertEquals(fightEvent.payload.attacker, fightingDeserializedEvent.payload.attacker)
            },
            {
                assertEquals(fightEvent.payload.defender, fightingDeserializedEvent.payload.defender)
            },
            {
                assertEquals(5, fightingDeserializedEvent.payload.remainingDefenderHealth)
            },
            {
                assertEquals(18, fightingDeserializedEvent.payload.remainingEnergy)
            }
        )
    }
}

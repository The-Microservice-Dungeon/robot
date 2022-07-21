package com.msd.testUtil

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.domain.DomainEvent
import com.msd.event.application.EventType
import com.msd.event.application.dto.*
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
import java.util.concurrent.TimeUnit

class EventTestUtils {

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

    inline fun <reified T> getNextEventOfTopic(
        consumerRecords: BlockingQueue<ConsumerRecord<String, String>>,
        topic: String
    ): DomainEvent<T> {
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topic, singleRecord.topic())
        return DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), T::class.java),
            singleRecord.headers()
        )
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

    fun checkMovementPayload(
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
                assert(expectedRobots.containsAll(payload.robots))
                assert(payload.robots.containsAll(expectedRobots))
            }
        )
    }

  /*  fun checkBlockPayload(
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
*/
    fun checkRegenerationPayload(
        expectedSuccess: Boolean,
        expectedMessage: String,
        expectedremainingEnergy: Int?,
        payload: RegenerationEventDTO
    ) {
        assertAll(
            "assert regeneration payload correct",
            {
                assertEquals(expectedSuccess, payload.success)
            },
            {
                assertEquals(expectedMessage, payload.message)
            },
            {
                assertEquals(expectedremainingEnergy, payload.remainingEnergy)
            }
        )
    }

/*    fun checkItemRepairPayload(
        expectedSuccess: Boolean,
        expectedMessage: String,
        expectedRobots: List<RepairEventRobotDTO>,
        payload: ItemRepairEventDTO
    ) {
        assertAll(
            "check item repair payload correct",
            {
                assertEquals(expectedSuccess, payload.success)
            },
            {
                assertEquals(expectedMessage, payload.message)
            },
            {
                assert(expectedRobots.containsAll(payload.robots))
                assert(payload.robots.containsAll(expectedRobots))
            }
        )
    }

    fun checkItemMovementPayload(
        expectedSuccess: Boolean,
        expectedMessage: String,
        expectedAssociatedMovementId: UUID?,
        payload: ItemMovementEventDTO
    ) {
        assertAll(
            "check item movement payload correct",
            {
                assertEquals(expectedSuccess, payload.success)
            },
            {
                assertEquals(expectedMessage, payload.message)
            },
            {
                assertEquals(expectedAssociatedMovementId, payload.associatedMovement)
            }
        )
    }
*/
    fun checkFightingPayload(
        expectedSuccess: Boolean,
        expectedMessage: String,
        expectedAttackerId: UUID?,
        expectedDefenderID: UUID?,
        expectedRemainingDefenderHealth: Int?,
        expectedRemainingEnergy: Int?,
        payload: FightingEventDTO
    ) {
        assertAll(
            "Check fighting payload correct",
            {
                assertEquals(expectedSuccess, payload.success)
            },
            {
                assertEquals(expectedMessage, payload.message)
            },
            {
                assertEquals(expectedAttackerId, payload.attacker)
            },
            {
                assertEquals(expectedDefenderID, payload.defender)
            },
            {
                assertEquals(expectedRemainingDefenderHealth, payload.remainingDefenderHealth)
            },
            {
                assertEquals(expectedRemainingEnergy, payload.remainingEnergy)
            }
        )
    }

    fun checkMiningPayload(
        expectedSuccess: Boolean,
        expectedMessage: String,
        expectedRemainingEnergy: Int?,
        expectedUpdatedInventory: Int?,
        expectedResource: String,
        payload: MiningEventDTO
    ) {
        assertAll(
            "Check mining payload correct",
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
                assertEquals(expectedUpdatedInventory, payload.updatedInventory)
            },
            {
                assertEquals(expectedResource, payload.resourceType)
            }
        )
    }
/*
    fun checkItemFightingPayload(
        expectedSuccess: Boolean,
        expectedMessage: String,
        expectedRemainingItemCount: Int?,
        expectedAssociatedFightsIds: List<UUID>,
        payload: ItemFightingEventDTO
    ) {
        assertAll(
            "check item fighting payload correct",
            {
                assertEquals(expectedSuccess, payload.success)
            },
            {
                assertEquals(expectedMessage, payload.message)
            },
            {
                assertEquals(expectedRemainingItemCount, payload.remainingItemCount)
            },
            {
                assert(expectedAssociatedFightsIds.containsAll(payload.associatedFights))
                assert(payload.associatedFights.containsAll(expectedAssociatedFightsIds))
            }
        )
    }
*/
    fun checkSpawnPayload(
        expectedPlayer: UUID,
        expectedRobots: List<UUID>,
        payload: SpawnEventDTO
    ) {
        assertAll(
            "check spawn event payload correct",
            {
                assertNotNull(payload.robotId)
            },
            {
                assertEquals(expectedPlayer, payload.playerId)
            },
            {
                assert(expectedRobots.containsAll(payload.otherSeeableRobots))
                assert(payload.otherSeeableRobots.containsAll(expectedRobots))
            }
        )
    }
}

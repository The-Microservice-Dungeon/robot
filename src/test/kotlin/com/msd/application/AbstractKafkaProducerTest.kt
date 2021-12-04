package com.msd.application

import com.msd.event.application.ProducerTopicConfiguration
import com.msd.testUtil.EventTestUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.ContainerTestUtils
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

abstract class AbstractKafkaProducerTest(
    private val embeddedKafka: EmbeddedKafkaBroker,
    private val topicConfig: ProducerTopicConfiguration
) {
    protected val eventTestUtils = EventTestUtils()

    protected lateinit var consumerRecords: BlockingQueue<ConsumerRecord<String, String>>

    internal lateinit var movementContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var neighboursContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var blockedContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var miningContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var fightingContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var resourceDistributionContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var regenerationContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var itemFightingContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var itemRepairContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var itemMovementContainer: KafkaMessageListenerContainer<String, String>

    @BeforeEach
    open fun setup() {
        consumerRecords = LinkedBlockingQueue()
    }

    protected fun startMovementContainer() {
        movementContainer =
            eventTestUtils.createMessageListenerContainer(embeddedKafka, topicConfig.ROBOT_MOVEMENT, consumerRecords)
        movementContainer.start()
        ContainerTestUtils.waitForAssignment(movementContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startNeighboursContainer() {
        neighboursContainer =
            eventTestUtils.createMessageListenerContainer(embeddedKafka, topicConfig.ROBOT_NEIGHBOURS, consumerRecords)
        neighboursContainer.start()
        ContainerTestUtils.waitForAssignment(neighboursContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startPlanetBlockedContainer() {
        blockedContainer =
            eventTestUtils.createMessageListenerContainer(embeddedKafka, topicConfig.ROBOT_BLOCKED, consumerRecords)
        blockedContainer.start()
        ContainerTestUtils.waitForAssignment(blockedContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startMiningContainer() {
        miningContainer =
            eventTestUtils.createMessageListenerContainer(embeddedKafka, topicConfig.ROBOT_MINING, consumerRecords)
        miningContainer.start()
        ContainerTestUtils.waitForAssignment(miningContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startFightingContainer() {
        fightingContainer =
            eventTestUtils.createMessageListenerContainer(embeddedKafka, topicConfig.ROBOT_FIGHTING, consumerRecords)
        fightingContainer.start()
        ContainerTestUtils.waitForAssignment(fightingContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startResourceDistributionContainer() {
        resourceDistributionContainer =
            eventTestUtils.createMessageListenerContainer(
                embeddedKafka,
                topicConfig.ROBOT_RESOURCE_DISTRIBUTION,
                consumerRecords
            )
        resourceDistributionContainer.start()
        ContainerTestUtils.waitForAssignment(resourceDistributionContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startRegenerationContainer() {
        regenerationContainer =
            eventTestUtils.createMessageListenerContainer(
                embeddedKafka,
                topicConfig.ROBOT_REGENERATION,
                consumerRecords
            )
        regenerationContainer.start()
        ContainerTestUtils.waitForAssignment(regenerationContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startItemFightingContainer() {
        itemFightingContainer = eventTestUtils.createMessageListenerContainer(
            embeddedKafka,
            topicConfig.ROBOT_ITEM_FIGHTING,
            consumerRecords
        )
        itemFightingContainer.start()
        ContainerTestUtils.waitForAssignment(itemFightingContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startItemRepairContainer() {
        itemRepairContainer =
            eventTestUtils.createMessageListenerContainer(embeddedKafka, topicConfig.ROBOT_ITEM_REPAIR, consumerRecords)
        itemRepairContainer.start()
        ContainerTestUtils.waitForAssignment(itemRepairContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startItemMovementContainer() {
        itemMovementContainer = eventTestUtils.createMessageListenerContainer(
            embeddedKafka,
            topicConfig.ROBOT_ITEM_MOVEMENT,
            consumerRecords
        )
        itemMovementContainer.start()
        ContainerTestUtils.waitForAssignment(itemMovementContainer, embeddedKafka.partitionsPerTopic)
    }
}

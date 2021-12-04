package com.msd.application

import com.msd.event.application.ProducerTopicConfiguration
import com.msd.testUtil.EventChecker
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.ContainerTestUtils
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

open class RobotKafkaTest(
    private val embeddedKafka: EmbeddedKafkaBroker,
    private val topicConfig: ProducerTopicConfiguration
) {
    protected val eventChecker = EventChecker()

    protected lateinit var consumerRecords: BlockingQueue<ConsumerRecord<String, String>>

    internal lateinit var movementContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var blockedContainer: KafkaMessageListenerContainer<String, String>
    internal lateinit var regenerationContainer: KafkaMessageListenerContainer<String, String>

    @BeforeEach
    open fun setup() {
        consumerRecords = LinkedBlockingQueue()
    }

    protected fun startMovementContainer() {
        movementContainer = eventChecker.createMessageListenerContainer(embeddedKafka, topicConfig.ROBOT_MOVEMENT, consumerRecords)
        movementContainer.start()
        ContainerTestUtils.waitForAssignment(movementContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startPlanetBlockedContainer() {
        blockedContainer =
            eventChecker.createMessageListenerContainer(embeddedKafka, topicConfig.ROBOT_BLOCKED, consumerRecords)
        blockedContainer.start()
        ContainerTestUtils.waitForAssignment(blockedContainer, embeddedKafka.partitionsPerTopic)
    }

    protected fun startRegenerationContainer() {
        regenerationContainer =
            eventChecker.createMessageListenerContainer(embeddedKafka, topicConfig.ROBOT_REGENERATION, consumerRecords)
        regenerationContainer.start()
        ContainerTestUtils.waitForAssignment(regenerationContainer, embeddedKafka.partitionsPerTopic)
    }
}

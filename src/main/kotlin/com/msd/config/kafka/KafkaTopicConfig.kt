package com.msd.config.kafka

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin

@Configuration
class KafkaTopicConfig {

    @Value(value = "\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapAddress: String

    @Value(value = "\${spring.kafka.topic.producer.robot-movement}")
    private lateinit var movementTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-neighbours}")
    private lateinit var neighboursTopic: String

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

    @Value(value = "\${spring.kafka.topic.consumer.round}")
    private lateinit var roundTopic: String

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs = mutableMapOf<String, Any>(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapAddress)
        return KafkaAdmin(configs)
    }

    @Bean
    fun movementTopic(): NewTopic = NewTopic(movementTopic, 1, 1)

    @Bean
    fun neighboursTopic(): NewTopic = NewTopic(neighboursTopic, 1, 1)

    @Bean
    fun planetBlockedTopic(): NewTopic = NewTopic(planetBlockedTopic, 1, 1)

    @Bean
    fun miningTopic(): NewTopic = NewTopic(miningTopic, 1, 1)

    @Bean
    fun fightingTopic() = NewTopic(fightingTopic, 1, 1)

    @Bean
    fun regenerationTopic() = NewTopic(regenerationTopic, 1, 1)

    @Bean
    fun itemFightingTopic() = NewTopic(itemFightingTopic, 1, 1)

    @Bean
    fun itemRepairTopic() = NewTopic(itemRepairTopic, 1, 1)

    @Bean
    fun itemMovementTopic() = NewTopic(itemMovementTopic, 1, 1)

    @Bean
    fun roundTopic(): NewTopic = NewTopic(roundTopic, 1, 1)
}

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

    @Value(value = "\${spring.kafka.topic.producer.robot}")
    private lateinit var robotTopic: String

    @Value(value = "\${spring.kafka.topic.consumer.round}")
    private lateinit var roundTopic: String

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs = mutableMapOf<String, Any>(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapAddress)
        return KafkaAdmin(configs)
    }

    @Bean
    fun robotTopic(): NewTopic = NewTopic(robotTopic, 1, 1)

    @Bean
    fun prodRoundTopic(): NewTopic = NewTopic(roundTopic, 1, 1)
}

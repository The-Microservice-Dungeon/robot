package com.msd.event.application

enum class ProducerTopicEnum(val topicName: String) {
    ROBOT_MOVEMENT("\${spring.kafka.topic.producer.robot-movement}"),
    ROBOT_NEIGHBOURS("\${spring.kafka.topic.producer.robot-neighbours}"),
    ROBOT_BLOCKED("\${spring.kafka.topic.producer.robot-blocked}"),
    ROBOT_MINING("\${spring.kafka.topic.producer.robot-mining}"),
    ROBOT_FIGHTING("\${spring.kafka.topic.producer.robot-fighting}"),
    ROBOT_REGENERATION("\${spring.kafka.topic.producer.robot-regeneration}"),
    ROBOT_ITEM_FIGHTING("\${spring.kafka.topic.producer.robot-item-fighting}"),
    ROBOT_ITEM_REPAIR("\${spring.kafka.topic.producer.robot-item-repair}"),
    ROBOT_ITEM_MOVEMENT("\${spring.kafka.topic.producer.robot-item-movement}"),
    ROBOT_RESOURCE_DISTRIBUTION("\${spring.kafka.topic.producer.robot-resource-distribution}")
}

package com.msd.event.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("topics")
class ProducerTopicConfiguration {
    @Value("robot-movement")
    lateinit var ROBOT_MOVEMENT: String

    @Value("robot-neighbours")
    lateinit var ROBOT_NEIGHBOURS: String

    @Value("robot-blocked")
    lateinit var ROBOT_BLOCKED: String

    @Value("robot-mining")
    lateinit var ROBOT_MINING: String

    @Value("robot-fighting")
    lateinit var ROBOT_FIGHTING: String

    @Value("robot-regeneration")
    lateinit var ROBOT_REGENERATION: String

    @Value("robot-item-fighting")
    lateinit var ROBOT_ITEM_FIGHTING: String

    @Value("robot-item-repair")
    lateinit var ROBOT_ITEM_REPAIR: String

    @Value("robot-item-movement")
    lateinit var ROBOT_ITEM_MOVEMENT: String

    @Value("robot-resource-distribution")
    lateinit var ROBOT_RESOURCE_DISTRIBUTION: String
}

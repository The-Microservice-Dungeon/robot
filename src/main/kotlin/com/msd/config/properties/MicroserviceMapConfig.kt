package com.msd.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@ConfigurationProperties(prefix = "microservice.map")
@PropertySource(value = ["classpath:application.yml"], factory = YamlPropertySourceFactory::class)
class MicroserviceMapConfig {

    lateinit var address: String
}

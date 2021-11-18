package com.msd.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync
@Profile("!no-async")
class AsyncConfig : AsyncConfigurer

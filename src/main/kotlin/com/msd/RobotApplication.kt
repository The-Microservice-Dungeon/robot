package com.msd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class RobotApplication

fun main(args: Array<String>) {
    runApplication<RobotApplication>(*args)
}

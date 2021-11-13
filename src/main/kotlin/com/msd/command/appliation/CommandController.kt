package com.msd.command.appliation

import com.msd.robot.application.RobotApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController("commands")
class CommandController(
    robotService: RobotApplicationService
) {

    @PostMapping
    fun receiveCommand(@RequestBody commands: List<String>): ResponseEntity<Any> {
    }
}

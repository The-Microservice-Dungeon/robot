package com.msd.command.application

import com.msd.robot.application.RobotApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController("commands")
class CommandController(
    val robotService: RobotApplicationService,
    val commandService: CommandApplicationService
) {

    @PostMapping
    fun receiveCommand(@RequestBody commandStrings: List<String>): ResponseEntity<Any> {
        val commands = commandService.parseCommandsFromStrings(commandStrings)
        // TODO now start command execution
        return ResponseEntity.ok("")
    }
}

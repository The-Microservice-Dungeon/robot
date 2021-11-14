package com.msd.command.application

import com.msd.robot.application.RobotApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands")
class CommandController(
    val robotService: RobotApplicationService,
    val commandService: CommandApplicationService
) {

    /**
     * Receives a list of commands in a string representation and executes them asynchronously.
     * @param commandDto A DTO containing the list of commands
     */
    @PostMapping
    fun receiveCommand(@RequestBody commandDto: CommandDTO): ResponseEntity<Any> {
        val commands = commandService.parseCommandsFromStrings(commandDto.commands)
        robotService.executeCommands(commands)
        return ResponseEntity.ok("Command batch accepted")
    }
}

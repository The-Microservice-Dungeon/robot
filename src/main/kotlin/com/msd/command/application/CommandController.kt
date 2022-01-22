package com.msd.command.application

import com.msd.robot.application.RobotApplicationService
import mu.KotlinLogging
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands")
class CommandController(
    val robotService: RobotApplicationService,
    val commandService: CommandApplicationService,
    val environment: Environment
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Receives a list of commands in a string representation and executes them asynchronously.
     *
     * @param commandDto A DTO containing the list of commands
     * @return ResponseEntity stating the success of the Command Queueing
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    fun receiveCommand(@RequestBody commandDto: CommandDTO): ResponseEntity<Any> {
        if (commandDto.commands.isNotEmpty()) {
            val commands = commandService.parseCommandsFromStrings(commandDto.commands)
            logger.info("Starting execution of command batch")
            if (!environment.acceptsProfiles("no-async")) {
                val thread = Thread { robotService.executeCommands(commands) }
                thread.start()
            } else {
                robotService.executeCommands(commands)
            }
        }
        return ResponseEntity.accepted().body("Command batch accepted")
    }
}

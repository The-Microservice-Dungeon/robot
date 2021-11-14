package com.msd.command.application

import com.msd.command.domain.Command
import org.springframework.stereotype.Service

@Service
class CommandApplicationService {

    fun parseCommandsFromStrings(commandStrings: List<String>): List<Command> {
        return commandStrings.map { parseCommand(it) }
    }

    fun parseCommand(commandString: String): Command {
        TODO()
    }
}

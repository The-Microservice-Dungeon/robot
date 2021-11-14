package com.msd.command.application

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class CommandParsingException(command: String) : RuntimeException(
    "The command '$command' could not be parsed. Please check for correct syntax."
)

package com.msd.command.application

class CommandParsingException(command: String, furtherInformation: String = "") : RuntimeException(
    "The command '$command' could not be parsed. Please check for correct syntax. " +
        "Further information: $furtherInformation"
)

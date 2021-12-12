package com.msd.admin.application

class GameplayVariablesParsingException(variable: String, furtherInformation: String = "") : RuntimeException(
    "The variable '$variable' could not be parsed. Please check for correct syntax. " +
        "Further information: $furtherInformation"
)

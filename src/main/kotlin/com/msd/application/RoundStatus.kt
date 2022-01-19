package com.msd.application

import com.fasterxml.jackson.annotation.JsonProperty

enum class RoundStatus {
    @JsonProperty("started")
    STARTED,
    @JsonProperty("command input ended")
    COMMAND_INPUT_ENDED,
    @JsonProperty("ended")
    ENDED
}

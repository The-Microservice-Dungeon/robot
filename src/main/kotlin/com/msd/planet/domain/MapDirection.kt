package com.msd.planet.domain

import com.fasterxml.jackson.annotation.JsonProperty

enum class MapDirection {
    @JsonProperty("north")
    NORTH,
    @JsonProperty("south")
    SOUTH,
    @JsonProperty("east")
    EAST,
    @JsonProperty("west")
    WEST
}

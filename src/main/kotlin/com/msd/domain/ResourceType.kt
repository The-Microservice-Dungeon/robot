package com.msd.domain

import com.fasterxml.jackson.annotation.JsonProperty

enum class ResourceType(val requiredMiningLevel: Int) {
    @JsonProperty("coal",)
    COAL(0),
    @JsonProperty("iron")
    IRON(1),
    @JsonProperty("gem")
    GEM(2),
    @JsonProperty("gold")
    GOLD(3),
    @JsonProperty("platin")
    PLATIN(4)
}

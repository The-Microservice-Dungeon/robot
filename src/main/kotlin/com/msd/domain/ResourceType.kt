package com.msd.domain

enum class ResourceType(val requiredMiningLevel: Int) {
    COAL(0),
    IRON(1),
    GEM(2),
    GOLD(3),
    PLATIN(4)
}

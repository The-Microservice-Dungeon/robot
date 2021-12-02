package com.msd.application

enum class EventType(val eventString: String) {
    MOVEMENT("movement"),
    PLANET_BLOCKED("planet_blocked"),
    REGENERATION("regeneration"),
    FIGHTING("fighting"),
    MINING("mining"),
    ITEM_REPAIR("item-repair"),
    ITEM_MOVEMENT("item-movement"),
    ITEM_FIGHTING("item-fighting")
}
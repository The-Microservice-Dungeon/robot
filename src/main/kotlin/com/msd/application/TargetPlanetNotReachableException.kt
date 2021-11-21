package com.msd.application

/**
 * Throw this Exception if a player requests a MovementCommand that can not be executed, because the target planet is
 * too far away.
 */
class TargetPlanetNotReachableException(s: String) : RuntimeException(s)

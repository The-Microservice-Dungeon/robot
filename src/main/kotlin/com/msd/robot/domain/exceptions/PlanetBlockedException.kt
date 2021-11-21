package com.msd.robot.domain.exceptions

/**
 * Throw this Exception if a [Robot] tries to leave a blocked [Planet]
 */
class PlanetBlockedException(s: String) : RuntimeException(s)

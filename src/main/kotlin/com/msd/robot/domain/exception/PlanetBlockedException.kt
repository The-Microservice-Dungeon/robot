package com.msd.robot.domain.exception

/**
 * Throw this Exception if a [Robot] tries to leave a blocked [Planet]
 */
class PlanetBlockedException(s: String) : RuntimeException(s)

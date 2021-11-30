package com.msd.robot.domain.exception

import com.msd.core.MovementFailureException

/**
 * Throw this Exception if a [Robot] tries to leave a blocked [Planet]
 */
class PlanetBlockedException(s: String, energyCost: Int) : MovementFailureException(s, energyCost)

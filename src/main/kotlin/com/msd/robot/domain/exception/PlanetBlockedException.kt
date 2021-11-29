package com.msd.robot.domain.exception

import com.msd.application.dto.MovementEventDTO
import com.msd.core.FailureException

/**
 * Throw this Exception if a [Robot] tries to leave a blocked [Planet]
 */
class PlanetBlockedException(s: String, movementEventDTO: MovementEventDTO) : FailureException(s, movementEventDTO)

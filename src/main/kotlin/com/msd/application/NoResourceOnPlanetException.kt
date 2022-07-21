package com.msd.application

import com.msd.config.kafka.core.FailureException
import java.util.*

class NoResourceOnPlanetException(planetId: UUID) :
    FailureException("Map Service did not return any resource on the planet $planetId")

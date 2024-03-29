package com.msd.robot.application.exception

import com.msd.config.kafka.core.FailureException
import java.util.*

class UnknownPlanetException(planetId: UUID) :
    FailureException("Map Service doesn't have a planet with the id $planetId")

package com.msd.robot.application.exception

import java.util.*

class UnknownPlanetException(planetId: UUID) :
    RuntimeException("Map Service doesn't have a planet with the id $planetId")

package com.msd.robot.application

import java.util.*

class UnknownPlanetException(planetId: UUID) :
    RuntimeException("Map Service doesn't have a planet with the id $planetId")

package com.msd.application

import java.util.*

class NoResourceOnPlanetException(planetId: UUID) :
    RuntimeException("Map Service did not return any resource on the planet $planetId")

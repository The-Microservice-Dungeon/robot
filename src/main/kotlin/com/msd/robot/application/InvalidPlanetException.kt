package com.msd.robot.application

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.*

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidPlanetException(planet: UUID) : RuntimeException("Invalid planet with UUID $planet")

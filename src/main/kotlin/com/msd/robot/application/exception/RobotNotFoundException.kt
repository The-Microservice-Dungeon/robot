package com.msd.robot.application.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class RobotNotFoundException(s: String) : RuntimeException(s)

package com.msd.robot.domain.exception

import com.msd.config.kafka.core.FailureException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class RobotNotFoundException(s: String) : FailureException(s)

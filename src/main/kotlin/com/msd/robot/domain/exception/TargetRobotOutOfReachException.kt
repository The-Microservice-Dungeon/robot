package com.msd.robot.domain.exception

import com.msd.config.kafka.core.FailureException

class TargetRobotOutOfReachException(s: String) : FailureException(s)

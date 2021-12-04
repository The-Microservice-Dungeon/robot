package com.msd.robot.domain.exception

import com.msd.core.FailureException

class TargetRobotOutOfReachException(s: String) : FailureException(s)

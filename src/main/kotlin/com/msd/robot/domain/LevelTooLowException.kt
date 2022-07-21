package com.msd.robot.domain

import com.msd.config.kafka.core.FailureException

class LevelTooLowException(s: String) : FailureException(s)

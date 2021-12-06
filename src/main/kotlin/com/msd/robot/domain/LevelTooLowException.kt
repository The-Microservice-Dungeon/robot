package com.msd.robot.domain

import com.msd.core.FailureException

class LevelTooLowException(s: String) : FailureException(s)

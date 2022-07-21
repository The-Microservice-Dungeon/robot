package com.msd.robot.domain.exception

import com.msd.config.kafka.core.FailureException

class NotEnoughEnergyException(s: String) : FailureException(s)

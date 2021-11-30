package com.msd.core

open class MovementFailureException(s: String, val energyCost: Int) : FailureException(s)

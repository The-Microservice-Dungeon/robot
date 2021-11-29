package com.msd.core

import com.msd.application.dto.EventDTO

open class FailureException(s: String, val eventDTO: EventDTO) : RuntimeException(s)

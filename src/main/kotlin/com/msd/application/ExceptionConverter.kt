package com.msd.application

import org.springframework.stereotype.Service
import java.util.*

@Service
class ExceptionConverter {

    /**
     * Convert the Exception into a corresponding Kafka Event
     */
    fun handle(exception: RuntimeException, transactionId: UUID) {
        println("ERROR: ${exception.message}")
    }
}

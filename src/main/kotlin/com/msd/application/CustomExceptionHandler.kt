package com.msd.application

import org.springframework.stereotype.Service
import java.util.*

@Service
class CustomExceptionHandler {

    fun handle(exception: RuntimeException, transactionId: UUID) {
    }
}

package com.msd.application

import com.msd.command.application.CommandBatchParsingException
import com.msd.command.application.CommandParsingException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class CommandExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(CommandParsingException::class)
    fun handleParsingException(parsingException: CommandParsingException): ResponseEntity<Any> {
        return ResponseEntity(parsingException.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(CommandBatchParsingException::class)
    fun handleParsingException(parsingException: CommandBatchParsingException): ResponseEntity<Any> {
        return ResponseEntity(parsingException.message, HttpStatus.BAD_REQUEST)
    }
}

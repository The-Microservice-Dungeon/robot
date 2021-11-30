package com.msd.application

import com.msd.command.application.CommandBatchParsingException
import com.msd.command.application.CommandParsingException
import com.msd.robot.application.exception.RobotNotFoundException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class RestAPIExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(CommandParsingException::class)
    fun handleParsingException(parsingException: CommandParsingException): ResponseEntity<Any> {
        return ResponseEntity(parsingException.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(CommandBatchParsingException::class)
    fun handleParsingException(parsingException: CommandBatchParsingException): ResponseEntity<Any> {
        return ResponseEntity(parsingException.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(RobotNotFoundException::class)
    fun handleRobotNotFoundException(robotNotFoundException: RobotNotFoundException) =
        ResponseEntity(robotNotFoundException.message, HttpStatus.NOT_FOUND)

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        return ResponseEntity("Request could not be accepted", HttpStatus.BAD_REQUEST)
    }
}

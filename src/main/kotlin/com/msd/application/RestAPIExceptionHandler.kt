package com.msd.application

import com.msd.command.application.CommandBatchParsingException
import com.msd.command.application.CommandParsingException
import com.msd.robot.domain.exception.RobotNotFoundException
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
        logger.info("Rejected request because of parsing error.")
        return ResponseEntity(parsingException.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(CommandBatchParsingException::class)
    fun handleParsingException(parsingException: CommandBatchParsingException): ResponseEntity<Any> {
        logger.info("Rejected request because of parsing error.")
        return ResponseEntity(parsingException.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(RobotNotFoundException::class)
    fun handleRobotNotFoundException(robotNotFoundException: RobotNotFoundException): ResponseEntity<Any> {
        logger.info("Request failed because no robot with the specified ID was found.")
        return ResponseEntity(robotNotFoundException.message, HttpStatus.NOT_FOUND)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.info("Unreadable HTTP Request: ${ex.message}")
        logger.info("More information: ${ex.mostSpecificCause}")
        logger.info(ex.stackTrace)

        return ResponseEntity("Request could not be accepted", HttpStatus.BAD_REQUEST)
    }
}

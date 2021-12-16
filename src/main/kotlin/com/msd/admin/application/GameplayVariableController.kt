package com.msd.admin.application

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class GameplayVariableController(
    val gameplayVariableApplicationService: GameplayVariableApplicationService
) {

    @PatchMapping("/gameplay-variables")
    fun patchGameplayVariables(@RequestBody gameplayVariablesDTO: GameplayVariablesDTO): ResponseEntity<Any> {
        val changedDTO = gameplayVariableApplicationService.patchGameplayVariables(gameplayVariablesDTO)
        return ResponseEntity.status(HttpStatus.OK).body(changedDTO)
    }
}

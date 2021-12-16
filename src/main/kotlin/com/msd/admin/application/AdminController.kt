package com.msd.admin.application

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminController(
    val gameplayVariableApplicationService: GameplayVariableApplicationService
) {

    @PatchMapping("/gameplay-variables")
    fun patchGameplayVariables(@RequestBody gameplayVariablesDTO: GameplayVariablesDTO): ResponseEntity<Any> {
        gameplayVariableApplicationService.patchGameplayVariables(gameplayVariablesDTO)
        return ResponseEntity.accepted().body("Gameplay variable patch accepted")
    }
}

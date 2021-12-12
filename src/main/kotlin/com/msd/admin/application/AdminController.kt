package com.msd.admin.application

import com.msd.robot.application.RobotApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminController(
    val robotService: RobotApplicationService,
    val adminApplicationService: AdminApplicationService
) {

    @PatchMapping("/gameplay-variables")
    fun patchGameplayVariables(@RequestBody gameplayVariablesDTO: GameplayVariablesDTO): ResponseEntity<Any> {
        val variables = adminApplicationService.parseVariablesFromStrings(gameplayVariablesDTO)
        adminApplicationService.patchGameplayVariables(variables)
        return ResponseEntity.accepted().body("Gameplay variable patch accepted")
    }
}

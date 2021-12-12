package com.msd.admin.application

import com.msd.admin.domain.GameplayVariablePatch
import org.springframework.stereotype.Service

@Service
class AdminApplicationService {
    fun patchGameplayVariables(patch: GameplayVariablePatch): GameplayVariablePatch {
        TODO("not implemented") //  To change body of created functions use File | Settings | File Templates.
    }

    fun parseVariablesFromStrings(dto: GameplayVariablesDTO): GameplayVariablePatch = TODO()
}

package com.msd.admin.domain

import org.springframework.data.repository.CrudRepository
import java.util.*

interface GameplayVariableRepository : CrudRepository<GameplayVariablePatch, UUID>

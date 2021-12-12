package com.msd.event.application

import org.springframework.data.repository.CrudRepository
import java.util.*

interface EventRepository : CrudRepository<ErrorEvent, UUID>

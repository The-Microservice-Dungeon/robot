package com.msd.robot.domain

import org.springframework.data.repository.CrudRepository
import java.util.*

interface RobotRepository : CrudRepository<Robot, UUID>

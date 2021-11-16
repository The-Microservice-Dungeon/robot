package com.msd.planet.domain

import com.msd.robot.domain.Robot
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany

@Entity
data class Planet(
    @Id
    val planetId: UUID
) {

    var blocked: Boolean = false

    // just for orphan removal
    @OneToMany(mappedBy = "robot", orphanRemoval = true)
    private val robots: List<Robot> = listOf()
}

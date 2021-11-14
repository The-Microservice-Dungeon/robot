package com.msd.planet.domain

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Planet(
    @Id
    val planetId: UUID
) {

    var blocked: Boolean = false
}

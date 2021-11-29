package com.msd.planet.domain

import com.msd.domain.ResourceType
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Planet(
    @Id
    val planetId: UUID
) {

    var blocked: Boolean = false
    var resourceType: ResourceType? = null
}

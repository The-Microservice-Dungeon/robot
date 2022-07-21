package com.msd.planet.domain

import com.msd.domain.ResourceType
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Planet(
    @Id
    @Type(type = "uuid-char")
    val planetId: UUID,
    var resourceType: ResourceType? = null
) {

   // var blocked: Boolean = false
}

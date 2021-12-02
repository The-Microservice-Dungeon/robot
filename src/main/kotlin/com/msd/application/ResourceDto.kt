package com.msd.application

import com.fasterxml.jackson.annotation.JsonProperty
import com.msd.domain.ResourceType

class ResourceDto(
    @JsonProperty("resource_type")
    val resourceType: ResourceType
)

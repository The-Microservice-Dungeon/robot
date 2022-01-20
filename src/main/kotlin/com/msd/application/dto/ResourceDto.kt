package com.msd.application.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.msd.domain.ResourceType

@JsonIgnoreProperties
class ResourceDto(
    @JsonProperty("resource_type")
    val resourceType: ResourceType
)

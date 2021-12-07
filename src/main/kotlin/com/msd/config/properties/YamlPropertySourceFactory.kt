package com.msd.config.properties

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory
import java.util.*

class YamlPropertySourceFactory : PropertySourceFactory {

    override fun createPropertySource(name: String?, encodedResource: EncodedResource): PropertySource<*> {
        val factory = YamlPropertiesFactoryBean()
        factory.setResources(encodedResource.resource)
        val properties: Properties = factory.getObject()!!
        return PropertiesPropertySource(encodedResource.resource.filename!!, properties)
    }
}

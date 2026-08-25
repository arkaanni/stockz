package dev.arkaan.configuration

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object Configuration {
    private val mapper = YAMLMapper().apply {
        registerKotlinModule()
    }

    val metadataConfiguration: MetadataConfiguration by lazy {
        mapper.readValue(
            javaClass.classLoader.getResource("metadata-configuration.yaml"),
            MetadataConfiguration::class.java
        )
    }
}
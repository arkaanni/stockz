package dev.arkaan.configuration

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper

object Configuration {
    private val mapper = YAMLMapper()

    val appConfiguration: AppConfiguration by lazy {
        mapper.readValue(
            javaClass.classLoader.getResource("app-configuration.yaml"),
            AppConfiguration::class.java
        )
    }

}
package dev.arkaan.configuration

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object Configuration {
    private val mapper = YAMLMapper()

    private val metadataConfiguration: MetadataConfiguration by lazy {
        mapper.readValue(
            javaClass.classLoader.getResource("metadata-configuration.yaml"),
            MetadataConfiguration::class.java
        )
    }

    private val appConfiguration: Properties by lazy {
        val propFile = Paths.get("app.properties")
        Files.newInputStream(propFile).use {
            Properties().apply {
                load(it)
            }
        }
    }

    fun negativeKeywords() = metadataConfiguration.getNegativeKeywords()

    fun adobeStockCategories() = metadataConfiguration
        .getCategories()
        .getAdobeStock()

    fun shutterStockCategories() = metadataConfiguration
        .getCategories()
        .getShutterStock()

    fun isWsl() = (appConfiguration.getProperty("gs.wsl") ?: "false") == "true"

    fun apiKey(): String? = appConfiguration.getProperty("gemini.api.key")

    fun geminiModels() = (appConfiguration.getProperty("gemini.model") ?: "gemini-3.1-flash-lite")
        .split(",")
        .toList()
}
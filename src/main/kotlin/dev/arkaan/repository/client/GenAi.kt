package dev.arkaan.repository.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.google.genai.types.Schema
import dev.arkaan.configuration.Category
import dev.arkaan.configuration.Configuration
import dev.arkaan.domain.GenerateResponse
import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.repository.AIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenAi : AIClient {
    private lateinit var client: Client

    override fun buildClient(apiKey: String) {
        Client.builder()
            .apiKey(apiKey)
            .build().also {
                client = it
            }
    }

    override suspend fun generateContent(images: List<ImageWithMetadata>): GenerateResponse =
        withContext(Dispatchers.IO) {
            val prompt = Part.fromText(buildPrompt(images.size))
            val contents = images.map { img ->
                Part.fromBytes(
                    img.file.readBytes(),
                    "image/${img.file.extension}"
                )
            }.toTypedArray()
            val res = client.models?.generateContent(
                "gemini-3.1-flash-lite",
                Content.fromParts(*contents, prompt),
                responseConfig
            )
            objectMapper.readValue(res?.text(), GenerateResponse::class.java)
        }

    companion object {
        private val objectMapper by lazy { jacksonObjectMapper() }
        private val negativeKeywords = Configuration.metadataConfiguration.getNegativeKeywords()
        private val adobeStockCategories = Configuration
            .metadataConfiguration
            .getCategories()
            .getAdobeStock()
            .joinToString(", ", transform = Category::getName)
        private val jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "metadata": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "imageIndex": { "type": "integer" },
                                "title": { "type": "string" },
                                "description": { "type": "string" },
                                "category": { "type": "string" },
                                "keywords": {
                                    "type": "array",
                                    "items": {
                                        "type": "string"
                                    }
                                }
                            },
                            "required": ["imageIndex", "title", "description", "category", "keywords"]
                        }
                    }
                },
                "required": ["metadata"]
            }
        """.trimIndent()

        private val responseConfig = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(Schema.fromJson(jsonSchema))
            .build()

        private fun buildPrompt(imageCount: Int) = """
            You are a microstock metadata expert.
            Analyze these $imageCount images in exact sequential order (from 0 to ${imageCount - 1}).
            Generate stock metadata for each image (75-100 letter title, 30-60 words description, category, and 45-49 keywords).
            Avoid using the words: ($negativeKeywords) for the metadata. Avoid repeating words.
            The categories are one of these: ($adobeStockCategories)
            """.trimIndent()

    }
}
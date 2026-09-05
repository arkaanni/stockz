package dev.arkaan.repository.client

import com.google.genai.kotlin.Client
import com.google.genai.kotlin.types.Blob
import com.google.genai.kotlin.types.Content
import com.google.genai.kotlin.types.GenerateContentConfig
import com.google.genai.kotlin.types.Part
import dev.arkaan.configuration.AdobeStockCategory
import dev.arkaan.configuration.Configuration
import dev.arkaan.domain.GenerateResponse
import dev.arkaan.domain.ImagePreview
import dev.arkaan.repository.AIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GenAi : AIClient {
    private lateinit var client: Client

    override fun buildClient(apiKey: String) {
        Client(apiKey = apiKey).also {
            client = it
        }
    }

    override suspend fun generateContent(size: Int, images: Flow<ImagePreview>): GenerateResponse =
        withContext(Dispatchers.IO) {
            val prompt = Part(text = buildPrompt(size))
            val contents = images.map { img ->
                Part(
                    inlineData = Blob(
                        data = img.blob,
                        mimeType = "image/${img.extension}"
                    )
                )
            }.toList()
            val res = client.models.generateContent(
                "gemini-3.1-flash-lite",
                Content(contents.plus(prompt)),
                responseConfig
            )
            res.text?.let { Json.decodeFromString<GenerateResponse>(it) } ?: GenerateResponse(listOf())
        }

    companion object {
        private val negativeKeywords = Configuration.appConfiguration.getNegativeKeywords()
        private val adobeStockCategories = Configuration
            .appConfiguration
            .getCategories()
            .getAdobeStock()
            .joinToString(", ", transform = AdobeStockCategory::getName)
        private val shutterStockCategories = Configuration
            .appConfiguration
            .getCategories()
            .getShutterStock()
            .joinToString(", ")

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
                                "first_category": { "type": "string" },
                                "second_category": { "type": "string" },
                                "keywords": {
                                    "type": "array",
                                    "items": {
                                        "type": "string"
                                    }
                                }
                            },
                            "required": ["imageIndex", "title", "description", "first_category", "second_category", "keywords"]
                        }
                    }
                },
                "required": ["metadata"]
            }
        """.trimIndent()

        private val responseConfig = GenerateContentConfig(
            temperature = 0.7,
            responseJsonSchema = Json.parseToJsonElement(jsonSchema)
        )

        private fun buildPrompt(imageCount: Int) = """
            You are a microstock metadata expert. 
            Analyze these $imageCount images in exact sequential order (from 0 to ${imageCount - 1}). 
            Generate stock metadata for each image (75-100 letter title, 30-60 words description, first category, second category, 
            and strictly 49 unique keywords). Avoid using the words: ($negativeKeywords) for the metadata. 
            The first category must be strictly one of these (${adobeStockCategories}). 
            The second category must be strictly one of these (${shutterStockCategories})
            """.trimIndent()

    }
}
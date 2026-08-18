package dev.arkaan.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.google.genai.types.Schema
import dev.arkaan.domain.GenerateResponse
import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.domain.MetadataResult
import dev.arkaan.domain.ResultNavigation
import dev.arkaan.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenerateService {
    private var apiKey: String? = null
    private var client: Client? = null
    private val negativeKeywords = arrayOf(
        "AI", "vector", "AI-generated", "ui", "ux", "ui/ux", "design", "graphic"
    )

    private val objectMapper by lazy { ObjectMapper().registerModule(KotlinModule.Builder().build()) }

    companion object {
        private val categories = mapOf(
            "Animals" to 1,
            "Buildings and Architecture" to 2,
            "Business" to 3,
            "Drinks" to 4,
            "The Environment" to 5,
            "States of Mind" to 6,
            "Food" to 7,
            "Graphic Resources" to 8,
            "Hobbies and Leisure" to 9,
            "Industry" to 10,
            "Landscapes" to 11,
            "Lifestyle" to 12,
            "People" to 13,
            "Plants and Flowers" to 14,
            "Culture and Religion" to 15,
            "Science" to 16,
            "Social Issues" to 17,
            "Sports" to 18,
            "Technology" to 19,
            "Transport" to 20,
            "Travel" to 21
        )
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

    }


    private fun buildPrompt(imageCount: Int) =
        """
            You are a microstock metadata expert.
            Analyze these $imageCount images in exact sequential order (from 0 to ${imageCount - 1}).
            Generate stock metadata for each image (75-100 letter title, 30-60 words description, category, and 45-49 keywords).
            Avoid using the words: (${negativeKeywords.joinToString(", ")}) for the metadata. Avoid repeating words.
            The categories are one of these: (${categories.keys.joinToString(",")})
        """.trimIndent()

    fun setApiKey(apiKey: String) {
        client = null
        this.apiKey = apiKey
        Client.builder()
            .apiKey(apiKey)
            .build().also {
                client = it
            }
    }

    fun getApiKey() = apiKey

    suspend fun generate(images: List<ImageWithMetadata>): List<ImageWithMetadata> = withContext(Dispatchers.IO) {
        val prompt = Part.fromText(buildPrompt(images.size))
        val contents = images.map { img ->
            Part.fromBytes(
                img.file.readBytes(),
                "image/${img.file.extension}"
            )
        }.toTypedArray()
        if (client == null) {
            println("No client found")
        }
        val res = client?.models?.generateContent(
            "gemini-3.1-flash-lite",
            Content.fromParts(*contents, prompt),
            responseConfig
        )
        val response = objectMapper.readValue(res?.text(), GenerateResponse::class.java)
        val newSnapshot = response.metadata.map {
            images[it.imageIndex].let { img ->
                img.title = it.title
                img.description = it.description
                img.category = it.category
                img.keywords = it.keywords.joinToString(", ")
                img
            }
        }
        val resultId = Repository.insertResult()
        Repository.insert(newSnapshot, resultId)
        newSnapshot
    }

    fun getResult(currentId: Int = 1, navigation: ResultNavigation): MetadataResult {
        val nextId = when (navigation) {
            ResultNavigation.FIRST -> 1
            ResultNavigation.NEXT -> Repository.getNextResultId(currentId)
            ResultNavigation.PREVIOUS -> 1
        }
        return Repository.getResult(nextId)
    }
}
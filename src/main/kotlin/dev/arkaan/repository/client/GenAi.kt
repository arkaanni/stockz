package dev.arkaan.repository.client

import com.google.genai.kotlin.Client
import com.google.genai.kotlin.ClientException
import com.google.genai.kotlin.ServerException
import com.google.genai.kotlin.types.*
import dev.arkaan.configuration.AdobeStockCategory
import dev.arkaan.configuration.Configuration
import dev.arkaan.domain.AIModel
import dev.arkaan.domain.GenerateResponse
import dev.arkaan.domain.ImagePreview
import dev.arkaan.domain.MetadataResponse
import dev.arkaan.repository.AIClient
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class GenAi : AIClient {
    private lateinit var client: Client
    private lateinit var model: String

    override fun buildClient(apiKey: String) {
        Client(apiKey = apiKey).also {
            client = it
        }
    }

    override fun setModel(model: String) {
        this.model = model.removePrefix("models/")
    }

    override suspend fun generateContent(size: Int, images: Flow<ImagePreview>): GenerateResponse =
        withContext(Dispatchers.IO) {
            LOGGER.info("Generating metadata for {} images", size)
            val prompt = Part(text = buildPrompt(size))
            val contents = images.map { img ->
                Part(
                    inlineData = Blob(
                        data = img.blob,
                        mimeType = "image/${img.extension}"
                    )
                )
            }.toList()
            try {
                val res = client.models.generateContent(
                    model,
                    Content(contents.plus(prompt)),
                    responseConfig
                )
                val response = res.text?.let { Json.decodeFromString<GeminiResponse>(it) } ?: GeminiResponse(listOf())
                GenerateResponse(metadata = response.metadata.asFlow())
            } catch (e: Exception) {
                LOGGER.error("Unable to generate content", e)
                val message = when (e) {
                    is ClientException -> {
                        when (e.code) {
                            400 -> "Kesalahan data"
                            401 -> "Unauthorized"
                            403 -> "Akses ditolak"
                            404 -> "Model not found"
                            429 -> "Tidak ada kuota (rate limited)"
                            else -> "Server error"
                        }
                    }

                    is ServerException -> {
                        when (e.code) {
                            503 -> "Server sedang sibuk untuk model: $model"
                            else -> "Server error"
                        }
                    }

                    is HttpRequestTimeoutException -> "Request timed out"
                    else -> "Server error"
                }
                GenerateResponse(success = false, errorMessage = message)
            }
        }

    override suspend fun getModels(): Flow<AIModel> = client.models.list(
        ListModelsConfig(
            queryBase = true
        )
    )
        .filter { it.name != null && it.displayName != null }
        .filter { it.supportedActions?.contains("generateContent") == true }
        .map { AIModel(name = it.name!!, displayName = it.displayName!!) }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GenAi::class.java)
        private val negativeKeywords = Configuration.negativeKeywords()
        private val adobeStockCategories = Configuration
            .adobeStockCategories()
            .joinToString(", ", transform = AdobeStockCategory::getName)
        private val shutterStockCategories = Configuration
            .shutterStockCategories()
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
            temperature = 0.4,
            responseJsonSchema = Json.parseToJsonElement(jsonSchema),
            httpOptions = HttpOptions(timeout = 1000 * 60 * 5)
        )

        private fun buildPrompt(imageCount: Int) = """
            You are a microstock metadata expert. 
            Analyze these $imageCount images in exact sequential order (from 0 to ${imageCount - 1}). 
            Generate stock metadata for each image (strictly 75-100 letter title, strictly 30-60 words description, first category, second category, 
            and strictly 49 unique keywords). Avoid using the words: ($negativeKeywords) for the metadata. 
            The first category must be strictly one of these (${adobeStockCategories}). 
            The second category must be strictly one of these (${shutterStockCategories})
            """.trimIndent()

    }
}

@Serializable
internal data class GeminiResponse(
    val metadata: List<MetadataResponse>,
)
package dev.arkaan

import com.google.genai.Client
import com.google.genai.types.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class Service {
    private var apiKey: String? = null
    private var client: Client? = null
    private val negativeKeywords = arrayOf(
        "AI", "vector", "AI-generated", "ui", "ux", "ui/ux", "design", "graphic"
    )


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

        private val schema = Schema.builder()
            .type(Type.Known.OBJECT)
            .properties(mapOf(
                "metadata" to Schema.builder()
                    .type(Type.Known.ARRAY)
                    .items(Schema.builder()
                        .type(Type.Known.OBJECT)
                        .properties(mapOf(
                            "imageIndex" to Schema.builder().type(Type.Known.INTEGER).build(),
                            "title" to Schema.builder().type(Type.Known.STRING).build(),
                            "description" to Schema.builder().type(Type.Known.STRING).build(),
                            "category" to Schema.builder().type(Type.Known.STRING).build(),
                            "keywords" to Schema.builder().type(Type.Known.ARRAY)
                                .items(Schema.builder().type(Type.Known.STRING).build())
                                .build()
                        )).build()
            ).build()))
            .build()

        private val responseConfig = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(schema)
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
        println("categories: ${categories.keys.joinToString(", ")}")
        println("apiKey = $apiKey")
        client = null
        this.apiKey = apiKey
        Client.builder()
            .apiKey(apiKey)
            .build().also {
                client = it
            }
        println("client created: $client")
    }

    suspend fun generate(files: List<File>): GenerateContentResponse? = withContext(Dispatchers.IO) {
        val prompt = Part.fromText(buildPrompt(files.size))
        val contents = files.map { file -> Part.fromBytes(
            file.readBytes(),
            "image/${file.extension}"
        ) }.toTypedArray()
        println(client)
        if (client == null) {
            println("No client found")
        }
        client?.models?.generateContent(
            "gemini-3.1-flash-lite",
            Content.fromParts(*contents, prompt),
            responseConfig
        )
    }
}
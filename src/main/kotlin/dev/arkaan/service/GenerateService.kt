package dev.arkaan.service

import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.domain.MetadataResult
import dev.arkaan.domain.ResultNavigation
import dev.arkaan.repository.AIClient
import dev.arkaan.repository.db.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenerateService(
    private val ai: AIClient,
) {
    private var apiKey: String? = null

    fun setApiKey(apiKey: String) {
        ai.buildClient(apiKey)
    }

    fun getApiKey() = apiKey

    suspend fun generate(images: List<ImageWithMetadata>): List<ImageWithMetadata> {
        val response = ai.generateContent(images)
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
        return newSnapshot
    }

    suspend fun getResult(
        currentId: Int = 1,
        navigation: ResultNavigation
    ): MetadataResult = withContext(Dispatchers.IO) {
        val nextId = when (navigation) {
            ResultNavigation.FIRST -> 1
            ResultNavigation.NEXT -> Repository.getNextResultId(currentId)
            ResultNavigation.PREVIOUS -> 1
        }
        Repository.getResult(nextId)
    }
}
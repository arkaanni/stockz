package dev.arkaan.service

import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.domain.MetadataRequest
import dev.arkaan.domain.MetadataResult
import dev.arkaan.repository.AIClient
import dev.arkaan.repository.db.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

class GenerateService(
    private val ai: AIClient,
    private val previewService: PreviewService
) {
    fun setApiKey(apiKey: String) {
        ai.buildClient(apiKey)
    }

    suspend fun generate(images: List<MetadataRequest>): Flow<ImageWithMetadata> {
        val response = ai.generateContent(images.size, buildRequest(images))
        val resultList = ArrayList<ImageWithMetadata>(images.size)
        return response.metadata.asFlow().map {
            with(images[it.imageIndex]) {
                ImageWithMetadata(
                    fileName = fileName,
                    title = it.title,
                    description = it.description,
                    category = it.category,
                    keywords = it.keywords.joinToString(","),
                    preview = previewService.getPreview(previewId)
                )
            }
        }.onEach {
            resultList.add(it)
        }.onCompletion {
            insertResult(resultList)
        }
    }

    private fun buildRequest(images: List<MetadataRequest>) =
        images
            .asFlow()
            .map { previewService.getPreview(it.previewId) }

    suspend fun getLastResultId(): Int? = Repository.getLastResultId()

    private suspend fun insertResult(results: List<ImageWithMetadata>) {
        val resultId = Repository.insertResult()
        Repository.insert(results, resultId)
    }

    suspend fun getResult(
        id: Int = 1,
    ): MetadataResult = Repository.getResult(id)

    suspend fun delete(id: Int): Int = Repository.delete(id)
}
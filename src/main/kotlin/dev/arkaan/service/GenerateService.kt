package dev.arkaan.service

import dev.arkaan.domain.*
import dev.arkaan.repository.AIClient
import dev.arkaan.repository.db.MetadataRepository
import kotlinx.coroutines.flow.*

class GenerateService(
    private val ai: AIClient,
    private val metadataRepository: MetadataRepository,
    private val previewService: PreviewService
) {
    fun setModel(model: String) {
        ai.setModel(model)
    }

    fun setApiKey(apiKey: String) {
        ai.buildClient(apiKey)
    }

    suspend fun getModels(): Flow<AIModel> = ai.getModels()

    suspend fun generate(images: List<StockImageWithMetadata>): GenerateResult {
        val response = ai.generateContent(images.size, buildRequest(images))
        val resultList = ArrayList<StockImageWithMetadata>(images.size)
        val metadataFlow = response.metadata?.map {
            with(images[it.imageIndex]) {
                Pair(
                    it.imageIndex,
                    StockImageWithMetadata(
                        data = StockImage(
                            fileName = data.fileName,
                            title = it.title,
                            description = it.description,
                            firstCategory = it.firstCategory,
                            secondaryCategory = it.secondCategory,
                            keywords = it.keywords.joinToString(", ")
                        ),
                        previewId = previewId
                    )
                )
            }
        }?.onEach {
            resultList.add(it.second)
        }?.onCompletion {
            insertResult(resultList)
        }
        return GenerateResult(
            success = response.success,
            errorMessage = response.errorMessage,
            metadata = metadataFlow
        )
    }

    private fun buildRequest(images: List<StockImageWithMetadata>) =
        images
            .asFlow()
            .map { previewService.getPreview(it.previewId) }

    suspend fun getLastResultId(): Int? = metadataRepository.getLastResultId()

    private suspend fun insertResult(results: List<StockImageWithMetadata>) {
        val resultId = metadataRepository.insertResult()
        metadataRepository.insert(results, resultId)
    }

    suspend fun getResult(id: Int = 1): MetadataResult = metadataRepository.getResult(id)

    suspend fun getResultForExport(id: Int) = metadataRepository.getResultForExport(id)

    suspend fun delete(id: Int): Int = metadataRepository.delete(id)
}
package dev.arkaan.service

import dev.arkaan.domain.*
import dev.arkaan.repository.AIClient
import dev.arkaan.repository.db.MetadataJdbi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GenerateService(
    private val ai: AIClient,
    private val metadataJdbi: MetadataJdbi,
    private val previewService: PreviewService,
    private val scope: CoroutineScope,
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
        }

        if (metadataFlow != null) {
            scope.launch(Dispatchers.IO) { insertResult(metadataFlow.map { it.second }) }
        }

        return GenerateResult(
            success = response.success,
            errorMessage = response.errorMessage,
            metadata = metadataFlow
        )
    }

    private fun buildRequest(images: List<StockImageWithMetadata>) =
        images
            .map {
                previewService.getPreview(it.previewId)
                    ?: throw RuntimeException("Image ${it.data.fileName} not found")
            }

    fun getLastResultId(): Int? = metadataJdbi.getLastResultId()

    private fun insertResult(results: Sequence<StockImageWithMetadata>) {
        val resultId = metadataJdbi.insertResult()
        metadataJdbi.insert(results, resultId)
    }

    suspend fun getResult(id: Int = 1): MetadataResult = metadataJdbi.getResult(id)

    fun getResultForExport(id: Int) = metadataJdbi.getResultForExport(id)

    suspend fun delete(id: Int): Int = metadataJdbi.delete(id)
}
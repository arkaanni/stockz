package dev.arkaan.repository

import dev.arkaan.domain.AIModel
import dev.arkaan.domain.GenerateResponse
import dev.arkaan.domain.ImagePreview
import kotlinx.coroutines.flow.Flow

interface AIClient {
    fun buildClient(apiKey: String)
    fun setModel(model: String)
    fun getModel(): String
    suspend fun generateContent(size: Int, images: List<ImagePreview>): GenerateResponse
    suspend fun getModels(): Flow<AIModel>
    fun cleanup()
}
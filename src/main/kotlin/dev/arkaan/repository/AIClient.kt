package dev.arkaan.repository

import dev.arkaan.domain.GenerateResponse
import dev.arkaan.domain.ImagePreview
import kotlinx.coroutines.flow.Flow

interface AIClient {
    fun buildClient(apiKey: String)
    suspend fun generateContent(size: Int, images: Flow<ImagePreview>): GenerateResponse
}
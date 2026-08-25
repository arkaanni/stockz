package dev.arkaan.repository

import dev.arkaan.domain.GenerateResponse
import dev.arkaan.domain.ImageWithMetadata

interface AIClient {
    fun buildClient(apiKey: String)
    suspend fun generateContent(images: List<ImageWithMetadata>): GenerateResponse
}
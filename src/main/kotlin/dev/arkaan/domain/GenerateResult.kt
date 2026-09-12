package dev.arkaan.domain

import kotlinx.coroutines.flow.Flow

data class GenerateResult(
    val success: Boolean = true,
    val errorMessage: String? = null,
    val metadata: Flow<Pair<Int, StockImageWithMetadata>>? = null
)
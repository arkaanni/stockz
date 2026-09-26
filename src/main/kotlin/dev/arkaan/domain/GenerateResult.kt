package dev.arkaan.domain

data class GenerateResult(
    val success: Boolean = true,
    val errorMessage: String? = null,
    val metadata: Sequence<Pair<Int, StockImageWithMetadata>>? = null
)
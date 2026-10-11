package dev.arkaan.domain

import kotlinx.coroutines.flow.Flow

data class MetadataResult(
    val images: Flow<StockImageWithMetadata>,
    val resultId: Int,
    val previousId: Int,
    val nextId: Int,
    val model: String? = ""
)
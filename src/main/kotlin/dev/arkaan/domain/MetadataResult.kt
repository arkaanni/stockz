package dev.arkaan.domain

data class MetadataResult(
    val images: List<StockImageWithMetadata>,
    val resultId: Int,
    val previousId: Int,
    val nextId: Int,
)
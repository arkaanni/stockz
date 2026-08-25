package dev.arkaan.domain

data class MetadataResult(
    val images: List<ImageWithMetadata>,
    val resultId: Int,
    val previousId: Int,
    val nextId: Int,
)
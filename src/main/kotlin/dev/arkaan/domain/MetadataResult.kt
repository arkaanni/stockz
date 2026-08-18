package dev.arkaan.domain

data class MetadataResult(
    val images: List<ImageWithMetadata>,
    val resultId: Int
)
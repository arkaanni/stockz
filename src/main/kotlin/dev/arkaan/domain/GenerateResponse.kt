package dev.arkaan.domain

import kotlinx.serialization.Serializable

@Serializable
data class GenerateResponse(
    val metadata: List<MetadataResponse>,
)
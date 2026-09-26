package dev.arkaan.domain

import kotlinx.serialization.Serializable

@Serializable
data class GenerateResponse(
    val metadata: Sequence<MetadataResponse>? = null,
    val success: Boolean = true,
    val errorMessage: String? = null,
)
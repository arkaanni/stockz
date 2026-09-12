package dev.arkaan.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class GenerateResponse(
    val metadata: Flow<MetadataResponse>? = null,
    val success: Boolean = true,
    val errorMessage: String? = null,
)
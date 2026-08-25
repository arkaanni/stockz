package dev.arkaan.domain

import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize
data class GenerateResponse(
    val metadata: List<MetadataResponse>,
)
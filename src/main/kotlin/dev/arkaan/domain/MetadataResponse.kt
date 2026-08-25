package dev.arkaan.domain

import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize
data class MetadataResponse(
    val imageIndex: Int,
    val title: String,
    val description: String,
    val category: String,
    val keywords: List<String>,
)


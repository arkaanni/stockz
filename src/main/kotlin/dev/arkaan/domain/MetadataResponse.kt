package dev.arkaan.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize
data class MetadataResponse(
    val imageIndex: Int,
    val title: String,
    val description: String,
    @JsonProperty("first_category")
    val firstCategory: String,
    @JsonProperty("second_category")
    val secondCategory: String,
    val keywords: List<String>,
)


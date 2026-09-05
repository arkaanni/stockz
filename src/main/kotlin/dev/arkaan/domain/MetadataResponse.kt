package dev.arkaan.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetadataResponse(
    val imageIndex: Int,
    val title: String,
    val description: String,
    @SerialName("first_category")
    val firstCategory: String,
    @SerialName("second_category")
    val secondCategory: String,
    val keywords: List<String>,
)


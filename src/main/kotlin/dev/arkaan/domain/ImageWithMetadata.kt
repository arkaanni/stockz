package dev.arkaan.domain

import java.io.File

data class ImageWithMetadata(
    var cacheKey: String,
    var title: String,
    var description: String,
    var category: String,
    var keywords: String,
    var file: File
)
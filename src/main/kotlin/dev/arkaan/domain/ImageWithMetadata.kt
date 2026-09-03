package dev.arkaan.domain

data class ImageWithMetadata(
    override var fileName: String,
    override var title: String,
    override var description: String,
    override var category: String,
    override var keywords: String,
    var preview: ImagePreview? = null
) : ImageRequest
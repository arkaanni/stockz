package dev.arkaan.domain

data class ImagePreview(
    val id: Int,
    val blob: ByteArray,
    val extension: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImagePreview

        if (id != other.id) return false
        if (!blob.contentEquals(other.blob)) return false
        if (extension != other.extension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + blob.contentHashCode()
        result = 31 * result + extension.hashCode()
        return result
    }
}

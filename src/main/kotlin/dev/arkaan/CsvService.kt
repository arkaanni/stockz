package dev.arkaan

object CsvService {
    fun generateCsv(images: List<ImageWithMetadata>) {
        val header = "Filename,Title,Keywords,Category,Releases"
        val rows = images.map { with(it) {
            "${file.name},$title,\"${keywords}\",$category,"
        } }.joinToString("\n")
        println(rows)
    }
}
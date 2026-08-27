package dev.arkaan.service

import dev.arkaan.configuration.Configuration
import dev.arkaan.domain.ImageWithMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Paths

object CsvService {
    private val userDir = System.getProperty("user.home")
    private val downloadPath = Paths.get(userDir, "Downloads")
    private val categoryMap = Configuration
        .metadataConfiguration
        .getCategories()
        .getAdobeStock()
        .associate { it.getName() to it.getCode() }

    suspend fun generateCsv(images: List<ImageWithMetadata>) = withContext(Dispatchers.IO) {
        val metadataCsv = File("${downloadPath.toAbsolutePath()}/metadata-adobestock_${System.currentTimeMillis()}.csv")
        metadataCsv.bufferedWriter(Charsets.UTF_8).use { writer ->
            val header = "Filename,Title,Keywords,Category,Releases\n"
            writer.write(header)
            images.forEach { image ->
                writer.write("${image.file.name},\"${image.title}\",\"${image.keywords}\",${categoryMap[image.category]},\n")
            }
        }
    }
}
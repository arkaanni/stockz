package dev.arkaan.service

import dev.arkaan.configuration.Configuration
import dev.arkaan.domain.StockImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Paths

class CsvService(
    private val generateService: GenerateService
) {

    companion object {
        private val userDir = System.getProperty("user.home")
        private val downloadPath = Paths.get(userDir, "Downloads")
        private val categoryMapAdobeStock = Configuration
            .adobeStockCategories()
            .associate { it.getName() to it.getCode() }
    }

    suspend fun generateCsv(resultId: Int) = withContext(Dispatchers.IO) {
        val images = generateService.getResultForExport(resultId)
        val adobeStock = async { generateCsvAdobeStock(images) }
        val shutterStock = async { generateCsvShutterStock(images) }
        val vecteezy = async { generateCsvVecteezy(images) }
        awaitAll(adobeStock, shutterStock, vecteezy)
    }

    private suspend fun generateCsvAdobeStock(images: Flow<StockImage>) {
        val metadataCsv = File("${downloadPath.toAbsolutePath()}/metadata-adobestock_${System.currentTimeMillis()}.csv")
        metadataCsv.bufferedWriter(Charsets.UTF_8).use { writer ->
            val header = "Filename,Title,Keywords,Category,Releases\n"
            writer.write(header)
            images.collect {
                withContext(Dispatchers.IO) {
                    writer.write("${it.fileName},\"${it.title}\",\"${it.keywords}\",${categoryMapAdobeStock[it.firstCategory]},\n")
                }
            }
        }
    }

    private suspend fun generateCsvShutterStock(images: Flow<StockImage>) {
        val metadataCsv =
            File("${downloadPath.toAbsolutePath()}/metadata-shutterstock_${System.currentTimeMillis()}.csv")
        metadataCsv.bufferedWriter(Charsets.UTF_8).use { writer ->
            val header = "Filename,Description,Keywords,Categories,Editorial,Mature Content,Illustration\n"
            writer.write(header)
            images.collect {
                withContext(Dispatchers.IO) {
                    writer.write("${it.fileName},\"${it.description}\",\"${it.keywords}\",${it.secondaryCategory},no,,yes\n")
                }
            }
        }
    }

    private suspend fun generateCsvVecteezy(images: Flow<StockImage>) {
        val metadataCsv = File("${downloadPath.toAbsolutePath()}/metadata-vecteezy_${System.currentTimeMillis()}.csv")
        metadataCsv.bufferedWriter(Charsets.UTF_8).use { writer ->
            val header = "Filename,Title,Description,Keywords,License,Id\n"
            writer.write(header)
            images.collect {
                withContext(Dispatchers.IO) {
                    writer.write("${it.fileName},\"${it.title}\",\"${it.description}\",\"${it.keywords}\",pro,\n")
                }
            }
        }
    }
}
package dev.arkaan.service

import dev.arkaan.configuration.Configuration
import dev.arkaan.domain.StockImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
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

    private suspend fun writeCsvContents(
        writer: BufferedWriter,
        header: String,
        images: Flow<StockImage>,
        row: (StockImage) -> String
    ) = withContext(Dispatchers.IO) {
        writer.use { out ->
            writeRow(out, header)
            images.collect {
                writeRow(out, row(it))
            }
        }
    }

    private fun writeRow(writer: BufferedWriter, row: String) {
        writer.write(row)
        writer.newLine()
    }

    private suspend fun generateCsvAdobeStock(images: Flow<StockImage>) {
        val metadataCsv = File("${downloadPath.toAbsolutePath()}/metadata-adobestock_${System.currentTimeMillis()}.csv")
        val header = "Filename,Title,Keywords,Category,Releases"
        writeCsvContents(metadataCsv.bufferedWriter(Charsets.UTF_8), header, images) {
            "${it.fileName},\"${it.title}\",\"${it.keywords}\",${categoryMapAdobeStock[it.firstCategory]},"
        }
    }

    private suspend fun generateCsvShutterStock(images: Flow<StockImage>) {
        val metadataCsv = File("${downloadPath.toAbsolutePath()}/metadata-shutterstock_${System.currentTimeMillis()}.csv")
        val header = "Filename,Description,Keywords,Categories,Editorial,Mature Content,Illustration"
        writeCsvContents(metadataCsv.bufferedWriter(Charsets.UTF_8), header, images) {
            "${it.fileName},\"${it.description}\",\"${it.keywords}\",${it.secondaryCategory},no,,yes"
        }
    }

    private suspend fun generateCsvVecteezy(images: Flow<StockImage>) {
        val metadataCsv = File("${downloadPath.toAbsolutePath()}/metadata-vecteezy_${System.currentTimeMillis()}.csv")
        val header = "Filename,Title,Description,Keywords,License,Id"
        writeCsvContents(metadataCsv.bufferedWriter(Charsets.UTF_8), header, images) {
            "${it.fileName},\"${it.title}\",\"${it.description}\",\"${it.keywords}\",pro,"
        }
    }
}
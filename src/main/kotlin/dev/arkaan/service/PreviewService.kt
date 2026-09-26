package dev.arkaan.service

import dev.arkaan.configuration.Configuration
import dev.arkaan.domain.StockImage
import dev.arkaan.domain.StockImageWithMetadata
import dev.arkaan.repository.db.PreviewJdbi
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.io.path.*

class PreviewService(
    private val previewJdbi: PreviewJdbi
) {
    companion object {
        private val isWsl = Configuration.isWsl()
        private val isLinux = System.getProperty("os.name").contains("Linux")
        private val gsCmd by lazy {
            when {
                isWsl -> arrayOf("wsl", "gs")
                else -> if (isLinux) arrayOf("gs") else arrayOf("gswin64c.exe")
            }
        }
    }

    fun getPreviewBlob(id: Int) = previewJdbi.getPreviewBlob(id) ?: ByteArray(0)

    fun getPreview(id: Int) = previewJdbi.getPreview(id)

    fun createPreviews(images: List<Path>): Flow<StockImageWithMetadata> = flow {
        val out = ByteArrayOutputStream(256 * 1024)
        val tempDir = Paths.get("temp").apply {
            if (notExists()) {
                createDirectory()
            }
        }
        for (image in images) {
            out.reset()
            val previewId = when (val checkExists = previewJdbi.checkIfPreviewExist(image.pathString)) {
                null -> {
                    val blob = createPreview(image, tempDir, out)
                    val extension = if (image.extension == "eps") "png" else image.extension
                    previewJdbi.insertPreview(image.pathString, blob, extension)
                }

                else -> if (checkExists.second == null) {
                    val blob = createPreview(image, tempDir, out)
                    previewJdbi.updatePreview(checkExists.first, blob)
                    checkExists.first
                } else {
                    checkExists.first
                }
            }
            emit(
                StockImageWithMetadata(
                    data = StockImage(
                        fileName = image.name,
                        title = "",
                        description = "",
                        firstCategory = "",
                        secondaryCategory = "",
                        keywords = "",
                    ),
                    previewId = previewId
                )
            )
        }
        tempDir.toFile().deleteRecursively()
    }.flowOn(Dispatchers.IO)

    private fun createPreview(image: Path, tempDir: Path, out: ByteArrayOutputStream): ByteArray? {
        if (image.extension == "eps") {
            Files.copy(convertEpsToPng(image, tempDir), out)
        } else {
            val fxImage = Image(
                image.toUri().toURL().toString(),
                1024.0, 1024.0,
                true, true
            )
            ImageIO.write(SwingFXUtils.fromFXImage(fxImage, null), "jpg", out)
        }
        return if (out.size() > 0) out.toByteArray() else null
    }

    private fun convertEpsToPng(file: Path, tempDir: Path): Path {
        val tempPath = Paths.get("${tempDir}/${file.nameWithoutExtension}.png")
        Files.deleteIfExists(tempPath)
        val tempFile = Files.createFile(tempPath)
        val gsProcess = ProcessBuilder(
            *gsCmd,
            "-dNOPAUSE",
            "-dBATCH",
            "-dEPSCrop",
            "-dUseCropBox",
            "-sDEVICE=png16m",
            "-dFirstPage=1",
            "-dLastPage=1",
            "-g2048x2048",
            "-dEPSFitPage",
            "-sOutputFile=${if (isWsl) tempFile.toWslPath() else tempFile.pathString}",
            if (isWsl) file.toWslPath() else file.pathString
        ).apply {
            redirectOutput(ProcessBuilder.Redirect.DISCARD)
            redirectError(ProcessBuilder.Redirect.DISCARD)
        }
        return try {
            gsProcess.start().waitFor()
            tempFile
        } catch (e: Exception) {
            tempFile
        }
    }

    fun Path.toWslPath(): String {
        val absolutePath = this.toAbsolutePath().normalize().toString()

        // Matches "C:\" or "c:\" and replaces with "/mnt/c/" thx Gemini xD
        return absolutePath.replace(Regex("""^([a-zA-Z]):[/\\]""")) { match ->
            "/mnt/${match.groupValues[1].lowercase()}/"
        }.replace('\\', '/')
    }
}
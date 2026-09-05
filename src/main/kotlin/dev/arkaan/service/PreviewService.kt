package dev.arkaan.service

import dev.arkaan.domain.StockImage
import dev.arkaan.domain.StockImageWithMetadata
import dev.arkaan.repository.db.PreviewRepository
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
    private val previewRepository: PreviewRepository
) {

    fun getPreviewBlob(id: Int) = previewRepository.getPreviewBlob(id)

    fun getPreview(id: Int) = previewRepository.getPreview(id)

    fun createPreviews(images: List<Path>): Flow<StockImageWithMetadata> = flow {
        val out = ByteArrayOutputStream(256 * 1024)
        val tempDir = Paths.get("temp").apply {
            if (notExists()) {
                createDirectory()
            }
        }
        for (image in images) {
            out.reset()
            val previewId = when (val previewId = previewRepository.checkIfPreviewExist(image.pathString)) {
                null -> {
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
                    previewRepository.insertPreview(image.pathString, out.toByteArray(), image.extension)
                }

                else -> previewId
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

    private fun convertEpsToPng(file: Path, tempDir: Path): Path {
        val tempPath = Paths.get("${tempDir}/${file.nameWithoutExtension}.png")
        Files.deleteIfExists(tempPath)
        val tempFile = Files.createFile(tempPath)
        val isLinux = System.getProperty("os.name").contains("Linux")
        val gsProcess = ProcessBuilder(
            if (isLinux) "gs" else "gswin64c.exe",
            "-dNOPAUSE",
            "-dBATCH",
            "-dEPSCrop",
            "-dUseCropBox",
            "-sDEVICE=png16m",
            "-dFirstPage=1",
            "-dLastPage=1",
            "-g2048x2048",
            "-dEPSFitPage",
            "-sOutputFile=${tempFile.pathString}",
            file.pathString,
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
}
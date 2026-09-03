package dev.arkaan.service

import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.repository.db.Repository
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

class PreviewService {

    fun getPreview(id: Int) = Repository.getPreview(id)

    fun createPreviews(images: List<Path>): Flow<ImageWithMetadata> = flow {
        val out = ByteArrayOutputStream(64 * 1024)
        val tempDir = Paths.get("temp").apply {
            if (notExists()) {
                createDirectory()
            }
        }
        for (image in images) {
            out.reset()
            val previewId = Repository.checkIfPreviewExist(image.pathString)
            val preview = if (previewId != null) {
                Repository.getPreview(previewId)
            } else {
                if (image.extension == "eps") {
                    Files.copy(convertEpsToPng(image, tempDir), out)
                } else {
                    val fxImage = Image(
                        image.toUri().toURL().toString(),
                        600.0, 600.0,
                        true, false
                    )
                    ImageIO.write(SwingFXUtils.fromFXImage(fxImage, null), "jpg", out)
                }
                Repository.insertPreview(image.pathString, out.toByteArray(), image.extension)
            }
            emit(
                ImageWithMetadata(
                    fileName = image.name,
                    title = "",
                    description = "",
                    category = "",
                    keywords = "",
                    preview = preview
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
            "-g3840x2160", "-dEPSFitPage",
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
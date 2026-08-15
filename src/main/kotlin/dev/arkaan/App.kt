package dev.arkaan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.collections.forEachIndexed

val colorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

data class ImageWithMetadata(
    var cacheKey: String,
    var title: String,
    var description: String,
    var category: String,
    var keywords: List<String>,
    var file: File
)

object PreviewImageCache {
    private const val MAX_ITEMS = 50
    private val cache = object : LinkedHashMap<String, ImageBitmap>(
        MAX_ITEMS, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<String?, ImageBitmap?>?): Boolean {
            return size > MAX_ITEMS
        }
    }

    @Synchronized
    fun get(key: String): ImageBitmap? = cache[key]

    @Synchronized
    fun put(key: String, bitmap: ImageBitmap) {
        cache[key] = bitmap
    }
}

@Composable
fun decodeBitmap(img: ImageWithMetadata): ImageBitmap {
    val cacheKey = "${img.file.absolutePath}"
    val cachedPreview = PreviewImageCache.get(cacheKey)
    if (cachedPreview != null) {
        return cachedPreview
    }

    val state = produceState<ImageBitmap?>(
        initialValue = null,
        key1 = img.file.absolutePath
    ) {
        withContext(Dispatchers.IO) {
            val imageInputStream = ImageIO.createImageInputStream(img.file)
            val imageReaders = ImageIO.getImageReaders(imageInputStream)
            val imageReader = imageReaders.next()
            try {
                val param = imageReader.apply {
                    input = imageInputStream
                }.let {
                    val origWidth = it.getWidth(0)
                    val origHeight = it.getHeight(0)

                    val subX = origWidth / (if (origWidth > origHeight) 240 else 180)
                    val subY = origHeight / (if (origHeight > origWidth) 240 else 180)
                    it.defaultReadParam.apply {
                        setSourceSubsampling(subX, subY, 0, 0)
                    }
                }
                value = imageReader.read(0, param).toComposeImageBitmap()
                PreviewImageCache.put(cacheKey, value!!)
                img.cacheKey = cacheKey
            } finally {
                imageReader.dispose()
                imageInputStream.close()
            }
        }

    }
    return state.value ?: ImageBitmap(200, 200, colorSpace = ColorSpaces.Srgb)
}

@JsonSerialize
data class MetadataResponse(
    val imageIndex: Int,
    val title: String,
    val description: String,
    val category: String,
    val keywords: List<String>,
)

@JsonSerialize
data class GenerateResponse(
    val metadata: List<MetadataResponse>,
)

fun main() {
    application {
        val state = rememberWindowState().apply {
            size = DpSize(1280.dp, 720.dp)
        }
        Window(
            onCloseRequest = ::exitApplication,
            title = "Stockz",
            state = state,
        ) {
            window.minimumSize = Dimension(1280, 720)
            MaterialTheme(
                colorScheme = colorScheme,
            ) {
                App()
            }
        }
    }
}

fun filePicker(fileNamFilter: FileFilter): JFileChooser = JFileChooser().apply {
    fileSelectionMode = JFileChooser.FILES_ONLY
    fileFilter = fileNamFilter
    isAcceptAllFileFilterUsed = false
    isMultiSelectionEnabled = true
}

@Preview
@Composable
fun App() {
    val dirPath = remember { mutableStateListOf<ImageWithMetadata>() }
    val fileNameFilter = FileNameExtensionFilter(
        "Image files (*.jpg, *.jpeg, *.png, *.eps)", "jpg", "jpeg", "png"
    )
    var apiKeyState by remember { mutableStateOf("") }
    var isTypingApiKey by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val objectMapper by lazy { ObjectMapper().registerModule(KotlinModule.Builder().build()) }
    var isProcessing by remember { mutableStateOf(false) }

    val service by lazy { Service() }

    Column {
        Row(
            Modifier.weight(2f)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp, 0.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row (
                Modifier.weight(2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                filePicker(fileNameFilter).also {
                                    it.showOpenDialog(null).let { ret ->
                                        if (ret == JFileChooser.APPROVE_OPTION) {
                                            if (it.selectedFiles.isNullOrEmpty()) {
                                                return@withContext
                                            }
                                            dirPath.clear()
                                            dirPath.addAll(it.selectedFiles.map { s -> ImageWithMetadata(
                                                title = "",
                                                description = "",
                                                category = "",
                                                keywords = listOf(),
                                                file = s,
                                                cacheKey = ""
                                            ) })
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isProcessing,
                ) {
                    Text("pilih gambar")
                }
                if (dirPath.isNotEmpty()) {
                    Text("${dirPath.size} image(s)", color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Row(
                Modifier.weight(3f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                OutlinedTextField(
                    value = apiKeyState,
                    modifier = Modifier.weight(4f),
                    enabled = isTypingApiKey,
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { apiKeyState = it },
                    placeholder = { Text("gemini api key") },
                )
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isTypingApiKey = !isTypingApiKey
                        if (apiKeyState.isNotBlank()) {
                            service.setApiKey(apiKeyState)
                        }
                    },
                    enabled = !isProcessing
                ) {
                    if (isTypingApiKey) {
                        Text("simpan")
                    } else {
                        Text("edit")
                    }
                }
            }
            Row(
                Modifier.weight(2f),
                horizontalArrangement = Arrangement.spacedBy(24.dp, alignment = Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isProcessing) {
                    CircularProgressIndicator()
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isProcessing = true
                            service.generate(dirPath.map(ImageWithMetadata::file))
                                .also { res ->
                                    println(res?.text())
                                    val response = objectMapper.readValue(res?.text(), GenerateResponse::class.java)
                                    val newSnapshot = response.metadata.map {
                                        dirPath[it.imageIndex].let { img ->
                                            img.title = it.title
                                            img.description = it.description
                                            img.category = it.category
                                            img.keywords = it.keywords
                                            img
                                        }
                                    }
                                    dirPath.clear()
                                    dirPath.addAll(newSnapshot)
                                    isProcessing = false
                                }
                        }
                    },
                    enabled = !dirPath.isEmpty() && !isProcessing && apiKeyState.isNotBlank()
                ) {
                    Text("generate")
                }
            }
        }

        LazyColumn(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .padding(24.dp)
                .weight(10f),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            items(
                count = dirPath.size,
                key = { dirPath[it].file.absolutePath }
            ) {
                Card {
                    Row(
                        Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.weight(1f)
                                .background(MaterialTheme.colorScheme.secondary)
                        ) {
                            Image(
                                bitmap = decodeBitmap(dirPath[it]),
                                contentDescription = "placeholder",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.aspectRatio(1f)
                            )
                        }
                        Column (
                            modifier = Modifier.weight(5f)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(dirPath[it].file.name)
                            Text("title: ${dirPath[it].title}")
                            Text("description: ${dirPath[it].description}")
                            Text("category: ${dirPath[it].category}")
                            Text("keywords: ${dirPath[it].keywords.joinToString(", ")}")

                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter

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
    var keywords: String,
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

                    if (origWidth > 240 && origHeight > 240) {
                        val subX = origWidth / (if (origWidth > origHeight) 240 else 180)
                        val subY = origHeight / (if (origHeight > origWidth) 240 else 180)
                        it.defaultReadParam.apply {
                            setSourceSubsampling(subX, subY, 0, 0)
                        }
                    }
                    it.defaultReadParam
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
    Repository.initialize()
    application {
        val state = rememberWindowState().apply {
            placement = WindowPlacement.Maximized
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

@Composable
fun ImageColumn(
    images: List<ImageWithMetadata>
) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        items(
            count = images.size,
            key = { images[it].file.absolutePath }
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
                            bitmap = decodeBitmap(images[it]),
                            contentDescription = "placeholder",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.aspectRatio(1f)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(5f)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(images[it].file.name)
                        Text("title: ${images[it].title}")
                        Text("description: ${images[it].description}")
                        Text("category: ${images[it].category}")
                        Text("keywords: ${images[it].keywords}")

                    }
                }
            }
        }
    }
}

@Composable
fun ResultPage(
    goBack: () -> Unit,
    images: List<ImageWithMetadata>,
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(onClick = { goBack() }) {
                Text("kembali")
            }
        }
        Box(
            Modifier.weight(2f)
                .padding(24.dp, 0.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ImageColumn(images)
        }
    }
}

@Composable
fun DirectoryChooserButton(
    coroutineScope: CoroutineScope,
    fileNameFilter: FileNameExtensionFilter,
    isProcessing: Boolean,
    addImages: (List<ImageWithMetadata>) -> Unit,
) {
    var count by remember { mutableStateOf(0) }
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
                                count = it.selectedFiles.size
                                addImages(it.selectedFiles.map { s ->
                                    ImageWithMetadata(
                                        title = "",
                                        description = "",
                                        category = "",
                                        keywords = "",
                                        file = s,
                                        cacheKey = ""
                                    )
                                })
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
    if (count > 0) {
        Text("$count image(s)", color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun ApiKeyField(
    isProcessing: Boolean,
    apiKey: String,
    setApiKey: (String) -> Unit,
) {
    var isTyping by remember { mutableStateOf(false) }
    var apiKeyState by remember { mutableStateOf(apiKey) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        OutlinedTextField(
            value = apiKeyState,
            modifier = Modifier.weight(4f),
            enabled = isTyping && !isProcessing,
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = { apiKeyState = it },
            placeholder = { Text("gemini api key") },
        )
        if (isTyping) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    isTyping = !isTyping
                    if (apiKeyState.isNotBlank()) {
                        try {
                            setApiKey(apiKeyState)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                enabled = !isProcessing
            ) {
                Text("simpan")
            }
        } else {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    isTyping = !isTyping
                },
                enabled = !isProcessing
            ) {
                Text("edit")
            }
        }
    }
}

@Composable
fun ProcessField(
    isProcessing: Boolean,
    enableGenerateButton: Boolean,
    generate: () -> Unit,
    showResult: () -> Unit,
) {
    if (isProcessing) {
        CircularProgressIndicator()
    }
    Button(
        onClick = { generate() },
        enabled = enableGenerateButton
    ) {
        Text("generate")
    }
    Button(
        onClick = { showResult() }
    ) {
        Text("lihat hasil")
    }
}

@Preview
@Composable
fun App() {
    val dirPath = remember { mutableStateListOf<ImageWithMetadata>() }
    val fileNameFilter = FileNameExtensionFilter(
        "Image files (*.jpg, *.jpeg, *.png, *.eps)", "jpg", "jpeg", "png"
    )
    var apiKeySet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var displayResult by remember { mutableStateOf(false) }

    val service = remember { Service() }

    if (displayResult) {
        ResultPage(
            images = service.getResult(),
            goBack = { displayResult = false }
        )
    } else {
        Column {
            Row(
                Modifier.weight(2f)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp, 0.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier.weight(2f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    DirectoryChooserButton(
                        coroutineScope = coroutineScope,
                        fileNameFilter = fileNameFilter,
                        isProcessing = isProcessing,
                        addImages = {
                            dirPath.clear()
                            dirPath.addAll(it)
                        }
                    )
                }
                Box(
                    Modifier.weight(3f).fillMaxWidth()
                ) {
                    ApiKeyField(
                        apiKey = service.getApiKey() ?: "",
                        isProcessing = isProcessing,
                        setApiKey = {
                            service.setApiKey(it)
                            apiKeySet = true
                        }
                    )
                }
                Row(
                    Modifier.weight(2f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, alignment = Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProcessField(
                        isProcessing = isProcessing,
                        enableGenerateButton = !isProcessing && apiKeySet && !dirPath.isEmpty(),
                        generate = {
                            isProcessing = true
                            coroutineScope.launch {
                                service.generate(dirPath)
                                    .also {
                                        dirPath.clear()
                                        dirPath.addAll(it)
                                        isProcessing = false
                                    }
                            }
                        },
                        showResult = {
                            displayResult = true
                        }
                    )
                }
            }

            Box(
                Modifier.weight(10f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                ImageColumn(dirPath)
            }
        }
    }
}

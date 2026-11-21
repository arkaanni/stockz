package dev.arkaan.view

import dev.arkaan.domain.AIModel
import dev.arkaan.domain.StockImageWithMetadata
import dev.arkaan.service.GenerateService
import dev.arkaan.service.PreviewService
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.util.StringConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.collections.isNotEmpty
import kotlin.collections.map

class MainScreen(
    private val generateService: GenerateService,
    private val previewService: PreviewService,
    private val coroutineScope: CoroutineScope,
    private val openHistoryScreen: () -> Unit
): Screen {
    private val fileList = FXCollections.observableArrayList<StockImageWithMetadata>()
    private val isProcessing = SimpleBooleanProperty(false)
    private val modelList = ComboBox<AIModel>()
    private val errorAlert = Alert(Alert.AlertType.ERROR)
    private val fileNameFilter = FileChooser.ExtensionFilter(
        "Image files (*.jpg, *.jpeg, *.png, *.eps)",
        "*.jpg", "*.jpeg", "*.png", "*.eps"
    )

    override fun render(): Parent {
        coroutineScope.launch {
            generateService.getModels()
                .onCompletion {
                    if (modelList.items.isNotEmpty()) {
                        with(modelList) {
                            val model = items[0]
                            generateService.setModel(model.name)
                            withContext(Dispatchers.JavaFx) {
                                value = model
                            }
                        }
                    }
                }.collect { model ->
                    withContext(Dispatchers.JavaFx) {
                        modelList.apply {
                            items.add(model)
                        }
                    }
                }
        }
        val contents = Contents().apply {
            items = fileList
            disableProperty().bind(isProcessing)
        }
        return VBox(
            Header(),
            contents
        )
    }

    private fun filePicker(fileNameFilter: FileChooser.ExtensionFilter): FileChooser = FileChooser().apply {
        title = "pilih gambar"
        extensionFilters.add(fileNameFilter)
    }

    @Suppress("FunctionName")
    fun Header() = GridPane().apply {
        padding = Insets(16.0)
        hgap = 16.0
        vgap = 16.0
        addRow(
            0,
            Button("pilih gambar").apply {
                disableProperty().bind(isProcessing)
                onAction = EventHandler {
                    filePicker(fileNameFilter).showOpenMultipleDialog(null).also {
                        fileList.clear()
                        isProcessing.set(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            if (it != null && it.isNotEmpty()) {
                                previewService.createPreviews(it.map { i -> i.toPath() }).collect { image ->
                                    withContext(Dispatchers.JavaFx) {
                                        fileList.add(image)
                                    }
                                    yield()
                                }
                            }
                            isProcessing.set(false)
                        }
                    }
                }
            },
            modelList.apply {
                converter = object : StringConverter<AIModel>() {
                    override fun toString(m: AIModel?): String? = m?.displayName
                    override fun fromString(string: String?): AIModel? = null
                }
                onAction = EventHandler {
                    generateService.setModel(this.value.name)
                }
                disableProperty().bind(isProcessing)
            },
            Button("generate").apply {
                disableProperty().bind(
                    Bindings.isEmpty(fileList)
                        .or(isProcessing)
                )
                onAction = EventHandler {
                    isProcessing.set(true)
                    coroutineScope.launch(Dispatchers.IO) {
                        val result = generateService.generate(fileList)
                        when (result.success) {
                            true -> {
                                result.metadata?.forEach {
                                    withContext(Dispatchers.JavaFx) {
                                        fileList[it.first] = it.second
                                    }
                                }
                                withContext(Dispatchers.JavaFx) {
                                    isProcessing.set(false)
                                }
                            }

                            else -> {
                                withContext(Dispatchers.JavaFx) {
                                    errorAlert.apply {
                                        contentText = result.errorMessage
                                        buttonTypes.setAll(ButtonType.OK)
                                        showAndWait()
                                        isProcessing.set(false)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            Button("lihat hasil").apply {
                disableProperty().bind(isProcessing)
                onAction = EventHandler {
                    openHistoryScreen()
                }
            },
            ProgressIndicator().apply {
                visibleProperty().bind(isProcessing)
                setMaxSize(16.0, 16.0)
            }
        )
    }

    @Suppress("FunctionName")
    fun Contents() = ListView<StockImageWithMetadata>().apply {
        cellFactory = {
            ImageListCell {
                previewService.getPreviewBlob(it)
            }
        }
        padding = Insets(8.0)
        HBox.setHgrow(this, Priority.NEVER)
        VBox.setVgrow(this, Priority.ALWAYS)
    }
}
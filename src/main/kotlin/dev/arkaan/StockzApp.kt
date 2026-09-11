package dev.arkaan

import dev.arkaan.configuration.Configuration
import dev.arkaan.domain.StockImageWithMetadata
import dev.arkaan.service.CsvService
import dev.arkaan.service.GenerateService
import dev.arkaan.service.PreviewService
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.ListView
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.javafx.JavaFx
import javax.imageio.ImageIO

class StockzApp : Application() {
    private val fileList = FXCollections.observableArrayList<StockImageWithMetadata>()
    private val coroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.JavaFx) }
    private val isProcessing = SimpleBooleanProperty(false)
    private val showHistoryScreen = SimpleBooleanProperty(false)
    private var historyStage: Stage? = null
    private val contents = Contents()

    private fun filePicker(fileNameFilter: FileChooser.ExtensionFilter): FileChooser = FileChooser().apply {
        title = "pilih gambar"
        extensionFilters.add(fileNameFilter)
    }

    @Suppress("FunctionName")
    fun Header() = GridPane().apply {
        val models = Configuration.geminiModels()
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
            ChoiceBox<String>().apply {
                items.addAll(models)
                value = models[0]
                generateService.setModel(models[0])
                onAction = EventHandler {
                    println(this.value)
                    generateService.setModel(this.value)
                }
            },
            Button("generate").apply {
                disableProperty().bind(
                    Bindings.isEmpty(fileList)
                        .or(isProcessing)
                )
                onAction = EventHandler {
                    isProcessing.set(true)
                    coroutineScope.launch(Dispatchers.IO) {
                        generateService.generate(fileList).onCompletion {
                            withContext(Dispatchers.JavaFx) {
                                isProcessing.set(false)
                            }
                        }.collect {
                            withContext(Dispatchers.JavaFx) {
                                fileList.set(it.first, it.second)
                            }
                            yield()
                        }
                    }
                }
            },
            Button("lihat hasil").apply {
                disableProperty().bind(isProcessing)
                onAction = EventHandler {
                    showHistoryScreen.set(true)
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

    override fun start(primaryStage: Stage) {
        val apiKey = Configuration.apiKey() ?: throw IllegalStateException("API key is not set")
        if (apiKey.isBlank()) {
            throw IllegalStateException("API key is not set")
        }
        generateService.setApiKey(apiKey)
        showHistoryScreen.addListener { _, _, isShow ->
            run {
                if (isShow) {
                    showHistoryScreen(primaryStage)
                } else {
                    historyStage?.close()
                    historyStage = null
                }
            }
        }

        contents.apply {
            items = fileList
            disableProperty().bind(isProcessing)
        }

        val vBox = VBox(
            Header(),
            contents
        )
        val scene = Scene(vBox)
        primaryStage.apply {
            title = "Stockz"
            this.scene = scene
            minWidth = 1100.0
            minHeight = 650.0
            show()
        }
    }

    fun showHistoryScreen(mainStage: Stage) {
        historyScreen.start(mainStage) {
            showHistoryScreen.set(false)
        }
    }

    companion object {
        private lateinit var previewService: PreviewService
        private lateinit var generateService: GenerateService
        private lateinit var csvService: CsvService
        private lateinit var historyScreen: HistoryScreen
        private val fileNameFilter = FileChooser.ExtensionFilter(
            "Image files (*.jpg, *.jpeg, *.png, *.eps)",
            "*.jpg", "*.jpeg", "*.png", "*.eps"
        )

        fun Text.bold(size: Double): Text {
            font = Font.font("", FontWeight.BOLD, size)
            return this
        }

        fun startApp(
            previewService: PreviewService,
            generateService: GenerateService,
            csvService: CsvService
        ) {
            this.previewService = previewService
            this.generateService = generateService
            this.csvService = csvService
            this.historyScreen = HistoryScreen(previewService, generateService, csvService)
            ImageIO.setUseCache(false)
            launch(StockzApp::class.java)
        }
    }
}
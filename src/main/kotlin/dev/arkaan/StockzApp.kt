package dev.arkaan

import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.repository.client.GenAi
import dev.arkaan.repository.db.Repository
import dev.arkaan.service.GenerateService
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx

class StockzApp : Application() {

    private val generateService = GenerateService(GenAi())
    private val fileList = FXCollections.observableArrayList<ImageWithMetadata>()
    private val isEditingApiKey = SimpleBooleanProperty(false)
    private val coroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.JavaFx) }
    private val isProcessing = SimpleBooleanProperty(false)
    private val showHistoryScreen = SimpleBooleanProperty(false)
    private var historyStage: Stage? = null

    private fun filePicker(fileNameFilter: FileChooser.ExtensionFilter): FileChooser = FileChooser().apply {
        title = "pilih gambar"
        extensionFilters.add(fileNameFilter)
    }

    override fun init() {
        Repository.initialize()
    }

    private fun Text.bold(size: Double): Text {
        font = Font.font("", FontWeight.BOLD, size)
        return this
    }

    @Suppress("FunctionName")
    fun ImagePreview(image: ImageWithMetadata) = ImageView(
        Image(
            image.file.toURI().toString(), 200.0, 200.0,
            true, true
        )
    )

    @Suppress("FunctionName")
    fun MetadataTable(image: ImageWithMetadata) = VBox(
        Text(image.file.name).bold(14.0),
        HBox(
            VBox(
                Text("title").bold(12.0),
                Text("description").bold(12.0),
                Text("category").bold(12.0),
                Text("keywords").bold(12.0)
            ).apply {
                spacing = 8.0
            },
            VBox(
                TextFlow(Text(image.title)),
                TextFlow(Text(image.description)),
                TextFlow(Text(image.category)),
                TextFlow(Text(image.keywords))
            ).apply {
                spacing = 8.0
            }
        ).apply {
            spacing = 16.0
        }
    ).apply {
        spacing = 12.0
    }

    @Suppress("FunctionName")
    fun Header() = GridPane().apply {
        val apiKey = SimpleStringProperty("")
        padding = Insets(16.0)
        hgap = 16.0
        vgap = 16.0
        addRow(
            0,
            Button("pilih gambar").apply {
                disableProperty().bind(isProcessing)
                onAction = EventHandler {
                    filePicker(fileNameFilter).showOpenMultipleDialog(null).let {
                        if (it != null && it.isNotEmpty()) {
                            fileList.apply {
                                clear()
                                it.map { f ->
                                    ImageWithMetadata(
                                        title = "",
                                        description = "",
                                        category = "",
                                        keywords = "",
                                        file = f,
                                        cacheKey = ""
                                    )
                                }.toList().also { images ->
                                    addAll(images)
                                }
                            }
                        }
                    }
                }
            },
            TextField().apply {
                promptText = "gemini api key"
                disableProperty().bind(isEditingApiKey.not())
                editableProperty().bind(isEditingApiKey)
                textProperty().addListener { _, _, newValue -> apiKey.set(newValue) }
            },
            Button().apply {
                disableProperty().bind(isProcessing)
                textProperty().bind(
                    Bindings.`when`(isEditingApiKey)
                        .then("simpan")
                        .otherwise("ubah")
                )
                onAction = EventHandler {
                    if (isEditingApiKey.get() && apiKey.get().isNotEmpty()) {
                        generateService.setApiKey(apiKey.get())
                    }
                    isEditingApiKey.set(!isEditingApiKey.get())
                }
            },
            Button("generate").apply {
                disableProperty().bind(
                    apiKey.isEmpty
                        .or(isEditingApiKey)
                        .or(Bindings.isEmpty(fileList))
                        .or(isProcessing)
                )
                onAction = EventHandler {
                    isProcessing.set(true)
                    coroutineScope.launch(Dispatchers.JavaFx) {
                        generateService.generate(fileList).run {
                            fileList.clear()
                            fileList.addAll(this)
                            isProcessing.set(false)
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
    fun Contents() = ListView<HBox>().apply {
        padding = Insets(8.0)
        HBox.setHgrow(this, Priority.NEVER)
        VBox.setVgrow(this, Priority.ALWAYS)
    }

    override fun start(primaryStage: Stage) {
//        setUserAgentStylesheet(PrimerDark().userAgentStylesheet)

        showHistoryScreen.addListener { _, _, isShow ->
            run {
                if (isShow) {
                    HistoryScreen(primaryStage)
                } else {
                    historyStage?.close()
                    historyStage = null
                }
            }
        }

        val contents = Contents()

        fileList.addListener(ListChangeListener { images ->
            images.list.map { image ->
                HBox().apply {
                    val widthProperty = this.widthProperty()
                    children.setAll(
                        StackPane(ImagePreview(image)).apply {
                            alignment = Pos.CENTER_LEFT
                            this.minWidthProperty().bind(widthProperty.multiply(0.2))
                        },
                        MetadataTable(image)
                    )
                    maxWidthProperty().bind(contents.widthProperty().subtract(72))
                    padding = Insets(16.0)
                }
            }.run {
                contents.items.setAll(this)
            }
        })

        val vBox = VBox(
            Header(),
            contents
        )
        val scene = Scene(vBox)
        primaryStage.apply {
            title = "Stockz"
            this.scene = scene
            minWidth = 1280.0
            minHeight = 800.0
            show()
        }
    }

    @Suppress("FunctionName")
    fun HistoryScreen(mainStage: Stage) {
        if (historyStage != null) return

        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.JavaFx)
        val currentId = SimpleIntegerProperty()
        val prevId = SimpleIntegerProperty()
        val nextId = SimpleIntegerProperty()
        val imageList = FXCollections.observableArrayList<ImageWithMetadata>()

        val historyContents = Contents()

        imageList.addListener(ListChangeListener {
            coroutineScope.launch(Dispatchers.JavaFx) {
                it.list.map { image ->
                    HBox().apply {
                        val widthProperty = this.widthProperty()
                        children.setAll(
                            StackPane(ImagePreview(image)).apply {
                                alignment = Pos.CENTER_LEFT
                                this.minWidthProperty().bind(widthProperty.multiply(0.2))
                            },
                            MetadataTable(image)
                        )
                        maxWidthProperty().bind(historyContents.widthProperty().subtract(72))
                        padding = Insets(16.0)
                    }
                }.run {
                    historyContents.items.setAll(this)
                }
            }
        })

        val historyScene = Scene(
            VBox(
                historyContents,
                HBox(
                    Button("prev").apply {
                        disableProperty().bind(currentId.isEqualTo(prevId))
                        onAction = EventHandler {
                            coroutineScope.launch {
                                generateService.getResult(prevId.get()).run {
                                    withContext(Dispatchers.JavaFx) {
                                        imageList.setAll(images)
                                        currentId.set(resultId)
                                        prevId.set(previousId)
                                        nextId.set(this@run.nextId)
                                    }
                                }
                            }
                        }
                    },
                    Button("next").apply {
                        disableProperty().bind(currentId.isEqualTo(nextId))
                        onAction = EventHandler {
                            coroutineScope.launch {
                                generateService.getResult(nextId.get()).run {
                                    withContext(Dispatchers.JavaFx) {
                                        imageList.setAll(images)
                                        currentId.set(resultId)
                                        prevId.set(previousId)
                                        nextId.set(this@run.nextId)
                                    }
                                }
                            }
                        }
                    }
                ).apply {
                    padding = Insets(16.0)
                    spacing = 8.0
                }
            ).apply {
                spacing = 4.0
            })
        historyStage = Stage().apply {
            minWidth = 1280.0
            minHeight = 800.0
            width = 1280.0
            height = 800.0
            title = "metadata"
            scene = historyScene
            initOwner(mainStage)
            initModality(Modality.WINDOW_MODAL)
            onShown = EventHandler {
                coroutineScope.launch {
                    generateService.getLastResultId()?.run {
                        generateService.getResult(this).run result@ {
                            imageList.setAll(images)
                            currentId.set(resultId)
                            prevId.set(previousId)
                            nextId.set(this@result.nextId)
                        }
                    }
                }
            }
            onCloseRequest = EventHandler {
                showHistoryScreen.set(false)
                historyStage?.close()
                historyStage = null
                scene = null
            }
            show()
        }
    }

    companion object {
        private val fileNameFilter = FileChooser.ExtensionFilter(
            "Image files (*.jpg, *.jpeg, *.png, *.eps)",
            "*.jpg", "*.jpeg", "*.png", "*.eps"
        )

        @JvmStatic
        fun main(args: Array<String>) {
            launch(StockzApp::class.java, *args)
        }
    }
}
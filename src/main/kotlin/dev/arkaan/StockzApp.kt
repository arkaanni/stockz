package dev.arkaan

import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.repository.client.GenAi
import dev.arkaan.repository.db.Repository
import dev.arkaan.service.CsvService
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
import javafx.scene.layout.*
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
            true, false, true
        )
    ).apply {
        fitWidth = 200.0
        fitHeight = 200.0
        isPreserveRatio = true
    }

    @Suppress("FunctionName")
    fun MetadataTable(image: ImageWithMetadata) = VBox(
        Text(image.file.name).bold(14.0),
        GridPane().apply {
            hgap = 16.0
            vgap = 8.0
            alignment = Pos.TOP_LEFT
            val widthProperty = widthProperty()
            addRow(
                0,
                TextFlow(
                    Text("title").bold(12.0).apply {
                        prefWidth(widthProperty.multiply(0.3).get())
                    }
                ),
                TextFlow(Text(image.title))
            )
            addRow(
                1,
                    TextFlow(
                        Text("description").bold(12.0)
                    ).apply {
                        minWidthProperty().bind(widthProperty.multiply(0.1))
                    },
                    TextFlow(Text(image.description))
            )
            addRow(
                2,
                TextFlow(
                    Text("category").bold(12.0)
                ).apply {
                    minWidthProperty().bind(widthProperty.multiply(0.1))
                },
                TextFlow(Text(image.category))
            )
            addRow(
                3,
                TextFlow(
                    Text("keywords").bold(12.0)
                ).apply {
                    minWidthProperty().bind(widthProperty.multiply(0.1))
                },
                TextFlow(Text(image.keywords))
            )
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
                        spacing = 8.0
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

        currentId.addListener { _, _, newId ->
            coroutineScope.launch {
                generateService.getResult(newId.toInt()).run {
                    withContext(Dispatchers.JavaFx) {
                        imageList.setAll(images)
                        currentId.set(resultId)
                        prevId.set(previousId)
                        nextId.set(this@run.nextId)
                    }
                }
            }
        }

        val historyScene = Scene(
            VBox(
                historyContents,
                HBox().apply {
                    val widthProperty = widthProperty()
                    padding = Insets(16.0)
                    children.setAll(
                        HBox(
                            Button("prev").apply {
                                disableProperty().bind(currentId.isEqualTo(prevId))
                                onAction = EventHandler {
                                    currentId.set(prevId.get())
                                }
                            },
                            Button("next").apply {
                                disableProperty().bind(currentId.isEqualTo(nextId))
                                onAction = EventHandler {
                                    currentId.set(nextId.get())
                                }
                            },
                            Button("hapus").apply {
                                onAction = EventHandler {
                                    coroutineScope.launch {
                                        generateService.delete(currentId.get()).run newId@{
                                            withContext(Dispatchers.JavaFx) {
                                                currentId.set(this@newId)
                                            }
                                        }
                                    }
                                }
                            }
                        ).apply {
                            spacing = 8.0
                            this.prefWidthProperty().bind(widthProperty.multiply(0.8))
                        },
                        HBox(
                            Button("csv").apply {
                                onAction = EventHandler {
                                    coroutineScope.launch {
                                        CsvService.generateCsv(imageList).run {
                                            Alert(Alert.AlertType.NONE).apply {
                                                contentText = "csv export selesai"
                                                buttonTypes.setAll(ButtonType.OK)
                                                showAndWait()
                                            }
                                        }
                                    }
                                }
                            }
                        ).apply {
                            alignment = Pos.CENTER_RIGHT
                            this.prefWidthProperty().bind(widthProperty.multiply(0.2))
                        }
                    )
                },
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
                        currentId.set(this)
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
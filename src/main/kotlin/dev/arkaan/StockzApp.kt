package dev.arkaan

import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.domain.ResultNavigation
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
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch

class StockzApp : Application() {

    private val generateService = GenerateService(GenAi())
    private val fileList = FXCollections.observableArrayList<ImageWithMetadata>()
    private val isEditingApiKey = SimpleBooleanProperty(false)
    private val coroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.JavaFx) }
    private val isProcessing = SimpleBooleanProperty(false)
    private val showHistoryScreen = SimpleBooleanProperty(false)
    private var historyStage: Stage? = null

    fun filePicker(fileNameFilter: FileChooser.ExtensionFilter): FileChooser = FileChooser().apply {
        title = "pilih gambar"
        extensionFilters.add(fileNameFilter)
    }

    override fun init() {
        Repository.initialize()
    }

    fun Text.bold(size: Double): Text {
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
            VBox().apply {
                spacing = 8.0
                children.addAll(
                    TextFlow(Text(image.title)),
                    TextFlow(Text(image.description)),
                    TextFlow(Text(image.category)),
                    TextFlow(Text(image.keywords))
                )
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
    fun Contents() = GridPane().apply {
        padding = Insets(16.0)
        vgap = 16.0
        hgap = 16.0
    }

    @Suppress("FunctionName")
    fun MainContent(contents: GridPane) = ScrollPane().apply {
        content = contents
        isFitToWidth = true
        isFitToHeight = true
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
            contents.run {
                children.clear()
                images.list.forEachIndexed { index, image ->
                    addRow(
                        index,
                        ImagePreview(image),
                        MetadataTable(image)
                    )
                }
            }
        })

        val vBox = VBox().apply {
            padding = Insets(16.0)
            spacing = 16.0
            children.addAll(
                Header(),
                MainContent(contents),
            )
        }
        val scene = Scene(vBox)
        primaryStage.apply {
            title = "Stockz"
            this.scene = scene
            minWidth = 1280.0
            minHeight = 800.0
            show()
        }
    }

    suspend fun fetchResult(
        currentId: Int,
        navigation: ResultNavigation,
        addRow: (Int, ImageWithMetadata) -> Unit
    ) = generateService.getResult(currentId, navigation).let {
        it.images.forEachIndexed { index, metadata -> addRow(index, metadata) }
        it.resultId
    }

    @Suppress("FunctionName")
    fun HistoryScreen(mainStage: Stage) {
        if (historyStage != null) return

        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.JavaFx)
        val currentId = SimpleIntegerProperty(1)

        val historyContents = Contents()
        val updateContents = { index: Int, metadata: ImageWithMetadata ->
            historyContents.addRow(
                index,
                ImagePreview(metadata),
                MetadataTable(metadata)
            )
        }

        val historyScene = Scene(
            VBox(
                MainContent(historyContents),
                HBox(
                    Button("prev").apply {
                        onAction = EventHandler {
                            historyContents.children.clear()
                            coroutineScope.launch {
                                fetchResult(currentId.get(), ResultNavigation.PREVIOUS, updateContents).run {
                                    currentId.set(this)
                                }
                            }
                        }
                    },
                    Button("next").apply {
                        onAction = EventHandler {
                            historyContents.children.clear()
                            coroutineScope.launch {
                                fetchResult(currentId.get(), ResultNavigation.NEXT, updateContents).run {
                                    currentId.set(this)
                                }
                            }
                        }
                    }
                ).apply {
                    padding = Insets(8.0)
                    spacing = 8.0
                }
            ).apply {
                padding = Insets(16.0)
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
                historyContents.run {
                    children.clear()
                    coroutineScope.launch {
                        fetchResult(currentId.get(), ResultNavigation.FIRST, updateContents)
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
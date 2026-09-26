package dev.arkaan

import dev.arkaan.domain.StockImageWithMetadata
import dev.arkaan.service.CsvService
import dev.arkaan.service.GenerateService
import dev.arkaan.service.PreviewService
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx

class HistoryScreen(
    private val previewService: PreviewService,
    private val generateService: GenerateService,
    private val csvService: CsvService,
    private val coroutineScope: CoroutineScope,
) {
    private var historyStage: Stage? = null
    private val currentId = SimpleIntegerProperty()
    private val prevId = SimpleIntegerProperty()
    private val nextId = SimpleIntegerProperty()
    private val imageList = FXCollections.observableArrayList<StockImageWithMetadata>()

    private val history = ListView<StockImageWithMetadata>().apply {
        cellFactory = {
            ImageListCell {
                previewService.getPreviewBlob(it)
            }
        }
        padding = Insets(8.0)
        HBox.setHgrow(this, Priority.NEVER)
        VBox.setVgrow(this, Priority.ALWAYS)
        items = imageList
    }

    private fun historyScene() = Scene(
        VBox(
            history,
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
                                            if (this@newId != currentId.get()) {
                                                currentId.set(this@newId)
                                            } else {
                                                imageList.clear()
                                            }
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
                                    csvService.generateCsv(currentId.get()).run {
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

    fun start(mainStage: Stage, onClose: () -> Unit) {
        currentId.set(0)
        val historyScene = historyScene()
        historyStage = Stage().apply {
            minWidth = 1100.0
            minHeight = 650.0
            title = "metadata"
            scene = historyScene
            initOwner(mainStage)
            initModality(Modality.WINDOW_MODAL)
            onShown = EventHandler {
                coroutineScope.launch {
                    generateService.getLastResultId()?.run {
                        withContext(Dispatchers.JavaFx) {
                            currentId.set(this@run)
                        }
                    }
                }
            }
            onHidden = EventHandler {
                onClose()
                imageList.clear()
                historyStage = null
            }
            show()
        }
    }

    init {
        currentId.addListener { _, _, newId ->
            imageList.clear()
            coroutineScope.launch {
                generateService.getResult(newId.toInt()).run {
                    images.collect { image ->
                        withContext(Dispatchers.JavaFx) {
                            imageList.add(image)
                        }
                        yield()
                    }
                    withContext(Dispatchers.JavaFx) {
                        currentId.set(resultId)
                        prevId.set(previousId)
                        this@HistoryScreen.nextId.set(nextId)
                    }
                }
            }
        }
    }
}
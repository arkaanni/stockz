package dev.arkaan.view

import dev.arkaan.AppContext
import dev.arkaan.domain.StockImageWithMetadata
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ListView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@Suppress("FunctionName")
class HistoryScreen(
    appContext: AppContext,
    private val stage: Stage,
    private val onClose: () -> Unit
): Screen {
    private val previewService = appContext.previewService()
    private val generateService = appContext.generateService()
    private val csvService = appContext.csvService()
    private val coroutineScope = appContext.coroutineScope()

    private val currentId = SimpleIntegerProperty()
    private val prevId = SimpleIntegerProperty()
    private val nextId = SimpleIntegerProperty()
    private val imageList = FXCollections.observableArrayList<StockImageWithMetadata>()
    private val resultModel = SimpleStringProperty()

    private val historyList = ListView<StockImageWithMetadata>().apply {
        cellFactory = {
            ImageListCell {
                previewService.getPreviewBlob(it)
            }
        }
        background = Background(BackgroundFill(Color.WHITE, null, null))
        padding = Insets(8.0)
        HBox.setHgrow(this, Priority.NEVER)
        VBox.setVgrow(this, Priority.ALWAYS)
        items = imageList
    }

    private fun ModelInfoText() = HBox(
        Text("model: "),
        Text().apply {
            textProperty().bind(resultModel)
        }
    ).apply {
        padding = Insets(8.0)
    }

    private fun PrevButton() = Button("prev").apply {
        disableProperty().bind(currentId.isEqualTo(prevId))
        onAction = EventHandler {
            currentId.set(prevId.get())
        }
    }

    private fun NextButton() = Button("next").apply {
        disableProperty().bind(currentId.isEqualTo(nextId))
        onAction = EventHandler {
            currentId.set(nextId.get())
        }
    }

    private fun DeleteButton() = Button("hapus").apply {
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

    private fun ExportCsvButton() = Button("csv").apply {
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

    override fun render(): Parent {
        stage.apply {
            minWidth = 1100.0
            minHeight = 650.0
            title = "metadata"
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
                currentId.set(-1)
            }
        }
        return VBox(
            ModelInfoText(),
            historyList,
            HBox().apply {
                val widthProperty = widthProperty()
                padding = Insets(16.0)
                children.setAll(
                    HBox(
                        PrevButton(),
                        NextButton(),
                        DeleteButton(),
                    ).apply {
                        spacing = 8.0
                        this.prefWidthProperty().bind(widthProperty.multiply(0.8))
                    },
                    HBox(
                        ExportCsvButton()
                    ).apply {
                        alignment = Pos.CENTER_RIGHT
                        this.prefWidthProperty().bind(widthProperty.multiply(0.2))
                    }
                )
            },
        ).apply {
            spacing = 4.0
        }
    }

    init {
        currentId.addListener { _, _, newId ->
            val idInt = newId.toInt()
            if (idInt > 0) {
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
                            resultModel.set(model)
                        }
                    }
                }
            }
        }
    }
}
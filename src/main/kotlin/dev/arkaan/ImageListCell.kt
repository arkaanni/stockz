package dev.arkaan

import dev.arkaan.StockzApp.Companion.bold
import dev.arkaan.domain.StockImageWithMetadata
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ListCell
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import java.io.ByteArrayInputStream

class ImageListCell(
    private val retrievePreview: (previewId: Int) -> ByteArray
) : ListCell<StockImageWithMetadata>() {

    private val fileName = Text().bold(14.0)
    private val metadataTable = MetadataTable()
    private val previewImage = ImageView().apply {
        fitWidth = 200.0
        fitHeight = 200.0
        minWidth = 100.0
        minHeight = 100.0
        isPreserveRatio = true
    }
    private val previewContainer = Pane().apply {
        alignment = Pos.CENTER_LEFT
        maxWidth = 200.0
        minWidth = 200.0
        maxHeight = 200.0
        minHeight= 200.0
        children.setAll(previewImage)
    }
    private val container = HBox().apply {
        padding = Insets(16.0)
        spacing = 16.0
        children.setAll(
            previewContainer,
            VBox(
                fileName,
                metadataTable
            ).apply {
                spacing = 12.0
            }
        )
    }

    init {
        listViewProperty().addListener { _, _, listView ->
            if (listView != null) {
                container.maxWidthProperty().bind(this.widthProperty().subtract(16.0))
            } else {
                container.maxWidthProperty().unbind()
            }
        }
    }

    override fun updateItem(item: StockImageWithMetadata?, empty: Boolean) {
        super.updateItem(item, empty)
        text = null
        if (empty || item == null) {
            previewImage.image = null
            metadataTable.clear()
            fileName.text = null
            graphic = null
            return
        }
        val preview = retrievePreview(item.previewId)
        fileName.text = item.data.fileName
        previewImage.image = Image(
            ByteArrayInputStream(preview),
            160.0,
            160.0,
            true,
            true
        )
        metadataTable.updateItem(item)
        graphic = container
    }
}
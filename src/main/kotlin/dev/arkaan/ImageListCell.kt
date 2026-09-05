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
        isPreserveRatio = true
    }
    private val previewContainer = Pane().apply {
        alignment = Pos.CENTER_LEFT
        children.setAll(previewImage)
    }
    private val container = HBox().apply {
        padding = Insets(16.0)
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
                container.maxWidthProperty().bind(this.widthProperty().subtract(48))
            } else {
                container.maxWidthProperty().unbind()
            }
        }
        previewContainer.apply {
            minWidthProperty().bind(container.widthProperty().multiply(0.2))
        }
    }

    override fun updateItem(item: StockImageWithMetadata?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            previewImage.image = null
            metadataTable.clear()
            graphic = null
            return
        }
        val preview = retrievePreview(item.previewId)
        fileName.text = item.data.fileName
        previewImage.image = Image(
            ByteArrayInputStream(preview)
        )
        metadataTable.updateItem(item)
        graphic = container
    }
}
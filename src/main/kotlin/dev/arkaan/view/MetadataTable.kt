package dev.arkaan.view

import dev.arkaan.StockzApp.Companion.bold
import dev.arkaan.domain.StockImageWithMetadata
import javafx.geometry.Pos
import javafx.scene.layout.GridPane
import javafx.scene.text.Text
import javafx.scene.text.TextFlow

class MetadataTable : GridPane() {
    private val title = Text()
    private val description = Text()
    private val category = Text()
    private val keywords = Text()

    init {
        hgap = 16.0
        vgap = 8.0
        alignment = Pos.TOP_LEFT
        addRow(
            0,
            TextFlow(
                Text("title").bold(12.0)
            ).apply {
                minWidthProperty().bind(this@MetadataTable.widthProperty().multiply(0.1))
            },
            TextFlow(title)
        )
        addRow(
            1,
            TextFlow(
                Text("description").bold(12.0)
            ).apply {
                minWidthProperty().bind(this@MetadataTable.widthProperty().multiply(0.1))
            },
            TextFlow(description)
        )
        addRow(
            2,
            TextFlow(
                Text("category").bold(12.0)
            ).apply {
                minWidthProperty().bind(this@MetadataTable.widthProperty().multiply(0.1))
            },
            TextFlow(category)
        )
        addRow(
            3,
            TextFlow(
                Text("keywords").bold(12.0)
            ).apply {
                minWidthProperty().bind(this@MetadataTable.widthProperty().multiply(0.1))
            },
            TextFlow(keywords)
        )
    }

    fun updateItem(image: StockImageWithMetadata) {
        title.text = image.data.title
        description.text = image.data.description
        category.text = "${image.data.firstCategory}, ${image.data.secondaryCategory}"
        keywords.text = "${image.data.keywords} (${image.data.keywords.split(", ").count()})"
    }

    fun clear() {
        title.text = ""
        description.text = ""
        category.text = ""
        keywords.text = ""
    }
}
package dev.arkaan.repository.model

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object ImageMetadata : Table("image_metadata") {
    val id = integer("id").autoIncrement()
    val generateId = integer("generate_id")
        .references(GenerateResult.id, onDelete = ReferenceOption.CASCADE)
    val fileName = varchar("file_name", 255)
    val title = varchar("title", 255)
    val description = varchar("description", 1000)
    val firstCategory = varchar("first_category", 50)
    val secondCategory = varchar("second_category", 50)
    val keywords = varchar("keywords", 1000)
    val previewId = integer("preview_id")
        .references(Preview.id, onDelete = ReferenceOption.NO_ACTION)

    override val primaryKey = PrimaryKey(id)
}
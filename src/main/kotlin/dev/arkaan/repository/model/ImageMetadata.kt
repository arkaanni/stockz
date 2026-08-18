package dev.arkaan.repository.model

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object ImageMetadata : Table("image_metadata") {
    val id = integer("id").autoIncrement()
    val generateId = integer("generate_id")
        .references(GenerateResult.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 255)
    val description = varchar("description", 1000)
    val category = varchar("category", 20)
    val keywords = varchar("keywords", 1000)
    val filePath = varchar("file_path", 255)
    val cacheKey = varchar("cache_key", 255)

    override val primaryKey = PrimaryKey(id)
}
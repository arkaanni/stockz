package dev.arkaan.repository.model

import org.jetbrains.exposed.v1.core.Table

object Preview : Table("previews") {
    val id = integer("id").autoIncrement()
    val path = varchar("path", 255).uniqueIndex()
    val blob = binary("blob")
    val extension = varchar("extension", 5)

    override val primaryKey = PrimaryKey(id)
}

data class PreviewResult(
    val id: Int,
    val filePath: String
)
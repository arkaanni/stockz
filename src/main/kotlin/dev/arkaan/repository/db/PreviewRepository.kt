package dev.arkaan.repository.db

import dev.arkaan.domain.ImagePreview
import dev.arkaan.repository.model.Preview
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class PreviewRepository(
    private val db: Database,
) {
    fun getPreviewBlob(id: Int) = transaction(db) {
        Preview.select(Preview.blob)
            .where { Preview.id eq id }
            .limit(1)
            .first()[Preview.blob]
    }

    fun getPreview(id: Int) = transaction(db) {
        Preview.selectAll()
            .where { Preview.id eq id }
            .limit(1)
            .first().let {
                ImagePreview(
                    it[Preview.id],
                    it[Preview.blob],
                    it[Preview.extension]
                )
            }
    }

    fun checkIfPreviewExist(filePath: String) = transaction(db) {
        Preview
            .select(Preview.id)
            .where {
                Preview.path eq filePath
            }.firstOrNull()?.get(Preview.id)
    }

    fun insertPreview(filePath: String, blob: ByteArray, extension: String): Int = transaction(db) {
        with(checkIfPreviewExist(filePath)) {
            if (this == null) {
                val id = Preview.insert {
                    it[Preview.path] = filePath
                    it[Preview.blob] = blob
                    it[Preview.extension] = extension
                } get Preview.id
                id
            } else {
                this
            }
        }
    }
}
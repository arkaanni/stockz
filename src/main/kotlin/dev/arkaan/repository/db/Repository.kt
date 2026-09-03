package dev.arkaan.repository.db

import dev.arkaan.domain.ImagePreview
import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.domain.MetadataResult
import dev.arkaan.repository.model.GenerateResult
import dev.arkaan.repository.model.ImageMetadata
import dev.arkaan.repository.model.Preview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Repository {
    private lateinit var db: Database

    fun initialize() {
        db = Database.connect(
            url = "jdbc:sqlite:./stockz.db?foreign_keys=on",
            driver = "org.sqlite.JDBC"
        )

        transaction(db) {
//            SchemaUtils.drop(GenerateResult, ImageMetadata)
            SchemaUtils.create(GenerateResult)
            SchemaUtils.create(ImageMetadata)
            SchemaUtils.create(Preview)
        }
    }

    fun insertResult(): Int = transaction(db) {
        GenerateResult.insert { } get GenerateResult.id
    }

    suspend fun getLastResultId(): Int? = suspendTransaction(db) {
        GenerateResult.select(GenerateResult.id)
            .orderBy(GenerateResult.id to SortOrder.DESC)
            .limit(1)
            .firstOrNull()?.get(GenerateResult.id)
    }

    suspend fun insert(images: List<ImageWithMetadata>, resultId: Int) = suspendTransaction(db) {
        ImageMetadata.batchInsert(images) { img ->
            this[ImageMetadata.fileName] = img.fileName
            this[ImageMetadata.previewId] = img.preview?.id!!
            this[ImageMetadata.title] = img.title
            this[ImageMetadata.description] = img.description
            this[ImageMetadata.category] = img.category
            this[ImageMetadata.keywords] = img.keywords
            this[ImageMetadata.generateId] = resultId
        }
    }

    suspend fun getNextResultId(currentId: Int, isGreater: Boolean): Int = suspendTransaction(db) {
        val order = if (isGreater) SortOrder.ASC else SortOrder.DESC
        val whereClause = if (isGreater) {
            GenerateResult.id greater currentId
        } else {
            GenerateResult.id less currentId
        }
        GenerateResult.select(GenerateResult.id)
            .where { whereClause }
            .orderBy(GenerateResult.id to order)
            .limit(1)
            .firstOrNull()?.get(GenerateResult.id) ?: currentId
    }

    suspend fun getResult(id: Int): MetadataResult = withContext(Dispatchers.IO) {
        val nextId = async { getNextResultId(id, false) }
        val prevId = async { getNextResultId(id, true) }
        val images = async {
            suspendTransaction(db) {
                ImageMetadata.selectAll()
                    .where { ImageMetadata.generateId eq id }
                    .map {
                        ImageWithMetadata(
                            fileName = it[ImageMetadata.fileName],
                            title = it[ImageMetadata.title],
                            description = it[ImageMetadata.description],
                            category = it[ImageMetadata.category],
                            keywords = it[ImageMetadata.keywords]
                        )
                    }
            }
        }
        MetadataResult(
            images = images.await(), resultId = id,
            nextId = nextId.await(), previousId = prevId.await()
        )
    }

    suspend fun delete(id: Int): Int {
        transaction(db) {
            GenerateResult.deleteWhere { GenerateResult.id eq id }
        }
        return withContext(Dispatchers.IO) {
            val nextIdAsync = async { getNextResultId(id, false) }
            val prevIdAsync = async { getNextResultId(id, true) }
            val nextId = nextIdAsync.await()
            val prevId = prevIdAsync.await()
            if (prevId != id) prevId else nextId
        }
    }

    fun getPreview(id: Int) = transaction(db) {
        Preview.selectAll()
            .where { Preview.id eq id }
            .limit(1)
            .first().let {
                ImagePreview(
                    it[Preview.id],
                    it[Preview.path],
                    it[Preview.blob],
                    it[Preview.extension]
                )
            }
    }

    private fun checkIfPreviewExist(filePath: String) =
        Preview
            .select(Preview.id)
            .where {
                Preview.path eq filePath
            }.firstOrNull()?.get(Preview.id)

    fun insertPreview(filePath: String, blob: ByteArray, extension: String): ImagePreview = transaction(db) {
        with(checkIfPreviewExist(filePath)) {
            if (this == null) {
                val id = Preview.insert {
                    it[Preview.path] = filePath
                    it[Preview.blob] = blob
                    it[Preview.extension] = extension
                } get Preview.id
                ImagePreview(id, filePath, blob, extension)
            } else {
                ImagePreview(this, filePath, blob, extension)
            }
        }
    }
}
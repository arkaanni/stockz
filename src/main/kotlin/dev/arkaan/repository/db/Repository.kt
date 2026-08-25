package dev.arkaan.repository.db

import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.domain.MetadataResult
import dev.arkaan.repository.model.GenerateResult
import dev.arkaan.repository.model.ImageMetadata
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
import java.io.File

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
        }
    }

    fun insertResult(): Int {
        return transaction(db) {
            GenerateResult.insert { } get GenerateResult.id
        }
    }

    suspend fun getLastResultId(): Int? = withContext(Dispatchers.IO) {
        transaction(db) {
            GenerateResult.select(GenerateResult.id)
                .orderBy(GenerateResult.id to SortOrder.DESC)
                .limit(1)
                .firstOrNull()?.get(GenerateResult.id)
        }
    }

    suspend fun insert(images: List<ImageWithMetadata>, resultId: Int) = withContext(Dispatchers.IO) {
        transaction(db) {
            ImageMetadata.batchInsert(images) { img ->
                this[ImageMetadata.title] = img.title
                this[ImageMetadata.description] = img.description
                this[ImageMetadata.category] = img.category
                this[ImageMetadata.keywords] = img.keywords
                this[ImageMetadata.filePath] = img.file.absolutePath
                this[ImageMetadata.cacheKey] = img.cacheKey
                this[ImageMetadata.generateId] = resultId
            }
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
                            title = it[ImageMetadata.title],
                            description = it[ImageMetadata.description],
                            category = it[ImageMetadata.category],
                            keywords = it[ImageMetadata.keywords],
                            cacheKey = it[ImageMetadata.cacheKey],
                            file = File(it[ImageMetadata.filePath]),
                        )
                    }
            }
        }
        MetadataResult(
            images = images.await(), resultId = id,
            nextId = nextId.await(), previousId = prevId.await()
        )
    }

    fun deleteAll() {
        transaction(db) {
            GenerateResult.deleteAll()
        }
    }
}
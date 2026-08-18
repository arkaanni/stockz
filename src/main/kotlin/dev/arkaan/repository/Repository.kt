package dev.arkaan.repository

import dev.arkaan.domain.ImageWithMetadata
import dev.arkaan.domain.MetadataResult
import dev.arkaan.repository.model.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.*
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

    fun insert(images: List<ImageWithMetadata>, resultId: Int) {
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

    fun getNextResultId(currentId: Int): Int = transaction(db) {
        GenerateResult.select(GenerateResult.id)
            .where { GenerateResult.id greater currentId }
            .limit(1)
            .singleOrNull()?.get(GenerateResult.id) ?: 1
    }

    fun getResult(nextId: Int): MetadataResult = transaction(db) {
        val images = ImageMetadata.selectAll()
            .where { ImageMetadata.generateId eq nextId }
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
        MetadataResult(images = images, resultId = nextId)
    }

    fun deleteAll() {
        transaction(db) {
            GenerateResult.deleteAll()
        }
    }
}
package dev.arkaan

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

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

object GenerateResult : Table("generate_result") {
    val id = integer("id").autoIncrement()

    override val primaryKey = PrimaryKey(id)
}

data class MetadataResult(
    val images: List<ImageWithMetadata>,
    val resultId: Int
)

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
            GenerateResult.insert {  } get GenerateResult.id
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
package dev.arkaan.repository.db

import dev.arkaan.repository.model.GenerateResult
import dev.arkaan.repository.model.ImageMetadata
import dev.arkaan.repository.model.Preview
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DB {

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

    fun getDb(): Database = db

}
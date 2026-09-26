package dev.arkaan.repository.db

import dev.arkaan.repository.model.GenerateResult
import dev.arkaan.repository.model.ImageMetadata
import dev.arkaan.repository.model.Preview
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlite3.SQLitePlugin
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteDataSource

object DB {
    private val dataSource by lazy { SQLiteDataSource().apply {
        url = "jdbc:sqlite:./stockz.db?foreign_keys=on"
    } }

    private lateinit var db: Database
    private lateinit var jdbi: Jdbi

    fun initialize() {
        db = Database.connect(
            url = "jdbc:sqlite:./stockz.db?foreign_keys=on",
            driver = "org.sqlite.JDBC"
        )

        dataSource.connection.use { connection ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(connection))
            Liquibase("databasechangelog.yaml", ClassLoaderResourceAccessor(), database).run {
                update(Contexts(), LabelExpression())
            }
        }
        jdbi = Jdbi.create(dataSource)
            .installPlugin(SQLitePlugin())

        transaction(db) {
//            SchemaUtils.drop(GenerateResult, ImageMetadata)
//            SchemaUtils.create(GenerateResult)
//            SchemaUtils.create(ImageMetadata)
//            SchemaUtils.create(Preview)
        }
    }

    fun getDb(): Database = db

    fun getJdbi(): Jdbi = jdbi

    fun close() {

    }
}
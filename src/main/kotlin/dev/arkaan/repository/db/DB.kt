package dev.arkaan.repository.db

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlite3.SQLitePlugin

import org.sqlite.SQLiteDataSource

object DB {
    private val dataSource by lazy {
        SQLiteDataSource().apply {
            url = "jdbc:sqlite:./stockz.db"
        }
    }

    private lateinit var jdbi: Jdbi

    fun initialize() {
        dataSource.connection.use { connection ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(connection))
            Liquibase("databasechangelog.yaml", ClassLoaderResourceAccessor(), database).run {
                update(Contexts(), LabelExpression())
            }
        }
        jdbi = Jdbi.create(dataSource)
            .installPlugin(SQLitePlugin())
    }

    fun getJdbi(): Jdbi = jdbi

    fun close() {

    }
}
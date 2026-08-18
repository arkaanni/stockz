package dev.arkaan.repository.model

import org.jetbrains.exposed.v1.core.Table

object GenerateResult : Table("generate_result") {
    val id = integer("id").autoIncrement()

    override val primaryKey = PrimaryKey(id)
}
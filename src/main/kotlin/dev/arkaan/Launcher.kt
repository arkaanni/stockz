package dev.arkaan

import dev.arkaan.repository.client.GenAi
import dev.arkaan.repository.db.DB
import dev.arkaan.repository.db.MetadataRepository
import dev.arkaan.repository.db.PreviewRepository
import dev.arkaan.service.CsvService
import dev.arkaan.service.GenerateService
import dev.arkaan.service.PreviewService

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DB.initialize()
            val db = DB.getDb()
            val previewRepository = PreviewRepository(db)
            val metadataRepository = MetadataRepository(db)
            val previewService = PreviewService(previewRepository)
            val generateService = GenerateService(GenAi(), metadataRepository, previewService)
            val csvService = CsvService(generateService)
            StockzApp.startApp(previewService, generateService, csvService)
        }
    }
}
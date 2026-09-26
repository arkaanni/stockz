package dev.arkaan

import dev.arkaan.repository.AIClient
import dev.arkaan.repository.client.GenAi
import dev.arkaan.repository.db.DB
import dev.arkaan.repository.db.MetadataJdbi
import dev.arkaan.repository.db.PreviewJdbi
import dev.arkaan.service.CsvService
import dev.arkaan.service.GenerateService
import dev.arkaan.service.PreviewService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.javafx.JavaFx

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DB.initialize()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.JavaFx)
            val jdbi = DB.getJdbi()
            val previewJdbi = PreviewJdbi(jdbi)
            val metadataJdbi = MetadataJdbi(jdbi)
            val previewService = PreviewService(previewJdbi)
            val aiClient: AIClient = GenAi()
            val generateService = GenerateService(aiClient, metadataJdbi, previewService, scope)
            val csvService = CsvService(generateService)
            StockzApp.startApp(previewService, generateService, csvService, scope)
        }
    }
}
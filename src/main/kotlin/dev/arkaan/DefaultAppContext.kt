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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.javafx.JavaFx

class DefaultAppContext: AppContext {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.JavaFx)
    private val jdbi by lazy { DB.getJdbi() }
    private val previewJdbi by lazy { PreviewJdbi(jdbi) }
    private val metadataJdbi by lazy { MetadataJdbi(jdbi) }
    private val previewService by lazy { PreviewService(previewJdbi) }
    private val aiClient: AIClient by lazy { GenAi() }
    private val generateService by lazy { GenerateService(aiClient, metadataJdbi, previewService, coroutineScope) }
    private val csvService by lazy { CsvService(generateService) }

    override fun coroutineScope(): CoroutineScope = coroutineScope

    override fun previewService(): PreviewService = previewService

    override fun generateService(): GenerateService = generateService

    override fun csvService(): CsvService = csvService

    override fun cleanUp() {
        coroutineScope.cancel()
        generateService.cleanUpClient()
    }
}
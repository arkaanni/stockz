package dev.arkaan

import dev.arkaan.service.CsvService
import dev.arkaan.service.GenerateService
import dev.arkaan.service.PreviewService
import kotlinx.coroutines.CoroutineScope

interface AppContext {
    fun coroutineScope(): CoroutineScope
    fun previewService(): PreviewService
    fun generateService(): GenerateService
    fun csvService(): CsvService
    fun cleanUp()
}
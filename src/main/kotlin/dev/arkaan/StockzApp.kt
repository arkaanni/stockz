package dev.arkaan

import dev.arkaan.configuration.Configuration
import dev.arkaan.view.HistoryScreen
import dev.arkaan.view.MainScreen
import javafx.application.Application
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Scene
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.stage.Stage
import org.slf4j.LoggerFactory

open class StockzApp : Application() {
    protected open val appContext = DefaultAppContext()
    private val mainScreen by lazy {
        MainScreen(
            appContext.generateService(),
            appContext.previewService(),
            appContext.coroutineScope(),
            openHistoryScreen = {
                showHistoryScreen.set(true)
            }
        )
    }
    private val historyScreen by lazy {
        HistoryScreen(
            appContext.previewService(),
            appContext.generateService(),
            appContext.csvService(),
            appContext.coroutineScope()
        )
    }

    private val showHistoryScreen = SimpleBooleanProperty(false)

    override fun start(primaryStage: Stage) {
        val apiKey = Configuration.apiKey() ?: throw IllegalStateException("API key is not set")
        if (apiKey.isBlank()) {
            LOGGER.error("API key is not set")
            throw IllegalStateException("API key is not set")
        }
        appContext.generateService().setApiKey(apiKey)
        showHistoryScreen.addListener { _, _, isShow ->
            run {
                if (isShow) {
                    showHistoryScreen(primaryStage)
                }
            }
        }
        val scene = Scene(mainScreen.render())
        primaryStage.apply {
            title = "Stockz"
            this.scene = scene
            minWidth = 1100.0
            minHeight = 650.0
            show()
        }
    }

    fun showHistoryScreen(mainStage: Stage) {
        historyScreen.start(mainStage) {
            showHistoryScreen.set(false)
        }
    }

    override fun stop() {
        try {
            appContext.cleanUp()
        } finally {
            super.stop()
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StockzApp::class.java)
        fun Text.bold(size: Double): Text {
            font = Font.font("", FontWeight.BOLD, size)
            return this
        }
    }
}
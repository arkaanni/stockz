package dev.arkaan

import dev.arkaan.configuration.Configuration
import dev.arkaan.view.HistoryScreen
import dev.arkaan.view.MainScreen
import dev.arkaan.view.Screen
import javafx.application.Application
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Scene
import javafx.stage.Stage
import org.slf4j.LoggerFactory

open class StockzApp : Application() {
    protected open val appContext = DefaultAppContext()
    private val showHistoryScreen = SimpleBooleanProperty(false)
    private val mainScreen: Screen by lazy {
        MainScreen(
            appContext,
            openHistoryScreen = {
                showHistoryScreen.set(true)
            }
        )
    }
    private val historyStage by lazy { Stage() }
    private val historyScreen by lazy {
        HistoryScreen(appContext, historyStage, onClose = {
            showHistoryScreen.set(false)
        })
    }

    override fun start(primaryStage: Stage) {
        val apiKey = Configuration.apiKey() ?: throw IllegalStateException("API key is not set")
        if (apiKey.isBlank()) {
            LOGGER.error("API key is not set")
            throw IllegalStateException("API key is not set")
        }
        appContext.generateService().setApiKey(apiKey)
        val mainScene = Scene(mainScreen.render())
        primaryStage.apply {
            title = "Stockz"
            this.scene = mainScene
            minWidth = 1100.0
            minHeight = 650.0
            show()
        }
        val historyScene = Scene(historyScreen.render())
        historyStage.apply {
            initOwner(primaryStage)
            scene = historyScene
        }
        showHistoryScreen.addListener { _, _, isShow ->
            run {
                if (isShow) {
                    historyStage.show()
                }
            }
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
    }
}
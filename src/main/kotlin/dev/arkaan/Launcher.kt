package dev.arkaan

import dev.arkaan.repository.db.DB
import javafx.application.Application

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DB.initialize()
            Application.launch(StockzApp::class.java, *args)
        }
    }
}
package dev.arkaan.view

import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text

fun Text.bold(size: Double): Text {
    font = Font.font("", FontWeight.BOLD, size)
    return this
}
package dev.arkaan.domain

data class StockImage(
    var fileName: String,
    var title: String,
    var description: String,
    var firstCategory: String,
    var secondaryCategory: String,
    var keywords: String
)
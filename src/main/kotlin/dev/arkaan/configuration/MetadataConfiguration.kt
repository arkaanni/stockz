package dev.arkaan.configuration

class MetadataConfiguration {
    private var categories: Categories? = null
    private var negativeKeywords: String? = null

    fun setCategories(categories: Categories) {
        this.categories = categories
    }

    fun setNegativeKeywords(negativeKeywords: String) {
        this.negativeKeywords = negativeKeywords
    }

    fun getCategories(): Categories = categories ?: throw IllegalStateException()
    fun getNegativeKeywords(): String = negativeKeywords ?: ""
}

class Categories {
    private var adobeStock: List<AdobeStockCategory>? = null
    private var shutterStock: List<String>? = null

    fun setAdobeStock(adobeStock: List<AdobeStockCategory>) {
        this.adobeStock = adobeStock
    }

    fun setShutterStock(shutterStock: List<String>) {
        this.shutterStock = shutterStock
    }

    fun getAdobeStock(): List<AdobeStockCategory> =
        adobeStock ?: throw IllegalStateException("Adobe stock categories not set")

    fun getShutterStock(): List<String> =
        shutterStock ?: throw IllegalStateException("Shutter stock categories not set")
}

class AdobeStockCategory {
    private var code: Int? = null
    private var name: String? = null

    fun setCode(code: Int) {
        this.code = code
    }

    fun getCode(): Int = code ?: throw IllegalStateException("No code set")

    fun setName(name: String) {
        this.name = name
    }

    fun getName(): String = name ?: throw IllegalStateException("No name set")
}
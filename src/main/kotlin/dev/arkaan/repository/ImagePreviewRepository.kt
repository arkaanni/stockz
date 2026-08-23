package dev.arkaan.repository

//import androidx.compose.ui.graphics.ImageBitmap
//
//object ImagePreviewRepository {
//    private const val MAX_ITEMS = 50
//    private val cache = object : LinkedHashMap<String, ImageBitmap>(
//        MAX_ITEMS, 0.75f, true
//    ) {
//        override fun removeEldestEntry(eldest: Map.Entry<String?, ImageBitmap?>?): Boolean {
//            return size > MAX_ITEMS
//        }
//    }
//
//    @Synchronized
//    fun get(key: String): ImageBitmap? = cache[key]
//
//    @Synchronized
//    fun put(key: String, bitmap: ImageBitmap) {
//        cache[key] = bitmap
//    }
//}
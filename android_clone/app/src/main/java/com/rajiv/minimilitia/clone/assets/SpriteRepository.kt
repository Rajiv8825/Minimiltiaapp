package com.rajiv.minimilitia.clone.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * SpriteRepository: Loads, caches, and provides authentic Mini Militia Bitmap assets.
 */
class SpriteRepository(private val context: Context) {

    private val tag = "SpriteRepository"
    private val bitmapCache = mutableMapOf<String, Bitmap>()
    private val composeBitmapCache = mutableMapOf<String, ImageBitmap>()

    fun loadAll() {
        // Automatically enumerate and load all sprites in assets/sprites/
        try {
            context.assets.list("sprites")?.forEach { fileName ->
                if (fileName.endsWith(".png", ignoreCase = true)) {
                    val key = fileName.substringBeforeLast(".png")
                    loadSprite("sprites/$fileName", key)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed listing sprites directory: ${e.message}")
        }

        // Automatically enumerate and load all maps in assets/maps/
        try {
            context.assets.list("maps")?.forEach { fileName ->
                if (fileName.endsWith(".png", ignoreCase = true)) {
                    val key = fileName.substringBeforeLast(".png")
                    loadSprite("maps/$fileName", key)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed listing maps directory: ${e.message}")
        }
    }

    private fun loadSprite(assetPath: String, cacheKey: String) {
        try {
            context.assets.open(assetPath).use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    bitmapCache[cacheKey] = bmp
                    composeBitmapCache[cacheKey] = bmp.asImageBitmap()
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to load sprite: $assetPath - ${e.message}")
        }
    }

    fun get(key: String): ImageBitmap? {
        val cleanKey = key.removeSuffix(".png")
        return composeBitmapCache[cleanKey]
    }

    fun getBitmap(key: String): Bitmap? {
        val cleanKey = key.removeSuffix(".png")
        return bitmapCache[cleanKey]
    }

    fun getFlippedHorizontal(key: String): ImageBitmap? {
        val flippedKey = "${key}_flipped"
        composeBitmapCache[flippedKey]?.let { return it }

        val original = getBitmap(key) ?: return null
        val matrix = Matrix().apply { preScale(-1.0f, 1.0f) }
        val flippedBmp = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        val imageBitmap = flippedBmp.asImageBitmap()
        composeBitmapCache[flippedKey] = imageBitmap
        bitmapCache[flippedKey] = flippedBmp
        return imageBitmap
    }

    fun clear() {
        bitmapCache.values.forEach { it.recycle() }
        bitmapCache.clear()
        composeBitmapCache.clear()
    }
}

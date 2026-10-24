package com.example.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class MapTileType {
    SATELLITE,      // صور الأقمار الصناعية المجانية (Esri World Imagery)
    STREET_MAP,     // خريطة الشوارع المفتوحة (OpenStreetMap)
    OFFLINE_GRID    // وضع توفير البيانات والعمل دون إنترنت
}

class TileManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val cacheDir = File(context.cacheDir, "satellite_tiles").apply { mkdirs() }
    private val memoryCache: LruCache<String, ImageBitmap> = object : LruCache<String, ImageBitmap>(80) {}

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Listener to request recomposition when new tile finishes loading
    var onTileLoaded: (() -> Unit)? = null

    fun getTile(zoom: Int, tileX: Int, tileY: Int, mapType: MapTileType): ImageBitmap? {
        if (mapType == MapTileType.OFFLINE_GRID) return null
        val key = "${mapType.name}_${zoom}_${tileX}_${tileY}"

        // 1. Check memory cache
        memoryCache.get(key)?.let { return it }

        // 2. Check disk cache
        val diskFile = File(cacheDir, "$key.png")
        if (diskFile.exists() && diskFile.length() > 0) {
            val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
            if (bitmap != null) {
                val imageBitmap = bitmap.asImageBitmap()
                memoryCache.put(key, imageBitmap)
                return imageBitmap
            }
        }

        // 3. Queue download in background
        fetchTileAsync(zoom, tileX, tileY, mapType, key, diskFile)
        return null
    }

    private val activeRequests = mutableSetOf<String>()

    private fun fetchTileAsync(
        zoom: Int,
        tileX: Int,
        tileY: Int,
        mapType: MapTileType,
        key: String,
        diskFile: File
    ) {
        synchronized(activeRequests) {
            if (activeRequests.contains(key)) return
            activeRequests.add(key)
        }

        scope.launch(Dispatchers.IO) {
            try {
                val url = when (mapType) {
                    MapTileType.SATELLITE ->
                        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$zoom/$tileY/$tileX"
                    MapTileType.STREET_MAP ->
                        "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"
                    MapTileType.OFFLINE_GRID -> return@launch
                }

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "AlAmirNetworkMap/1.0 (Android; Offline-First)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            FileOutputStream(diskFile).use { it.write(bytes) }
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                val imageBitmap = bitmap.asImageBitmap()
                                memoryCache.put(key, imageBitmap)
                                withContext(Dispatchers.Main) {
                                    onTileLoaded?.invoke()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fallback to offline rendering
            } finally {
                synchronized(activeRequests) {
                    activeRequests.remove(key)
                }
            }
        }
    }

    // Get total cached tiles size in MB for the Settings screen
    fun getCachedDataSizeMB(): Double {
        return try {
            val bytes = cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            bytes.toDouble() / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }

    // Clear disk cache
    fun clearCache() {
        memoryCache.evictAll()
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }
}

package com.example.speech.android

import android.content.Context
import com.example.BuildConfig
import com.example.core.logging.AppLogger
import com.example.speech.content.PracticeCatalog
import com.example.speech.content.PracticeCatalogParser
import java.io.IOException

/**
 * Loads the practice catalogue from the bundled content pack (`assets/content/`, ARC-03 §6.2) plus the built-in
 * quick practice. Release builds leave out items still `PENDING_REVIEW` (V-10); debug builds show them with a
 * "Pending review" badge. Call off the main thread.
 */
object AssetPracticeCatalog {

    private const val TAG = "AssetPracticeCatalog"

    fun load(context: Context, includePendingReview: Boolean = BuildConfig.DEBUG): PracticeCatalog {
        val assets = context.assets
        fun read(path: String): String? = try {
            assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            null
        }
        val techniques = read("content/techniques.json")
        val activityFiles = runCatching { assets.list("content/activities")?.toList().orEmpty() }
            .onFailure { AppLogger.w(TAG, "Could not list content activities", it) }
            .getOrDefault(emptyList())
            .filter { it.endsWith(".json") }
            .sorted()
        val activities = activityFiles.mapNotNull { read("content/activities/$it") }
        return PracticeCatalogParser.parse(techniques, activities, includePendingReview)
    }
}

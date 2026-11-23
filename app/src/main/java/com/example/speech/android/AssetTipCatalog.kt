package com.example.speech.android

import android.content.Context
import com.example.BuildConfig
import com.example.core.logging.AppLogger
import com.example.speech.feedback.TipDefinition
import com.example.speech.feedback.TipPackLoadResult
import com.example.speech.feedback.TipPackLoader
import com.example.speech.feedback.TipPackOptions
import com.example.speech.feedback.TipSelector
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Coach tips from the bundled content pack (`assets/content/tips.json`, ARC-03 §6.1–§6.2, §6.9).
 *
 * The pack is optional until EP-09 authors it: with no file, or a file that fails validation, the
 * built-in tips ([TipSelector.CATALOG]) are used, so feedback never breaks because of content. A
 * rejected pack is logged (rule ids and tip ids only, no content text) so the content build can be fixed.
 * Call off the main thread: it reads and parses an asset.
 */
object AssetTipCatalog {

    const val TIPS_ASSET = "content/tips.json"
    private const val TAG = "AssetTipCatalog"

    fun load(context: Context): List<TipDefinition> {
        val json = try {
            context.assets.open(TIPS_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: FileNotFoundException) {
            return TipSelector.CATALOG // No pack yet (EP-09): built-in tips.
        } catch (e: IOException) {
            AppLogger.w(TAG, "Could not read $TIPS_ASSET; using built-in tips", e)
            return TipSelector.CATALOG
        }
        return when (val result = TipPackLoader.load(json, TipPackOptions(releaseBuild = !BuildConfig.DEBUG))) {
            is TipPackLoadResult.Loaded -> result.tips
            is TipPackLoadResult.Rejected -> {
                val summary = result.violations.take(10).joinToString { "${it.rule}:${it.itemId ?: "-"}" }
                AppLogger.w(TAG, "$TIPS_ASSET rejected (${result.violations.size} problems: $summary); using built-in tips")
                TipSelector.CATALOG
            }
        }
    }
}

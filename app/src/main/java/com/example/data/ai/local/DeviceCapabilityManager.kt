package com.example.data.ai.local

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs

// ---------------------------------------------------------------------------
// Device profile tier (Stage 1.2: Lightweight on-device intelligence)
// ---------------------------------------------------------------------------

/**
 * Coarse device capability tier used to gate which AI features are enabled.
 *
 * | Tier     | Typical RAM  | Min API | Enabled capabilities                              |
 * |----------|--------------|---------|---------------------------------------------------|
 * | BASIC    | < 2 GB       | â‰¥ 24    | Pure DSP audio analysis, modular grammar, templates|
 * | STANDARD | 2 â€“ 4 GB     | â‰¥ 24    | Basic + 1 lightweight speech model (Whisper/Moon) |
 * | ADVANCED | â‰¥ 4 GB       | â‰¥ 28    | Lightweight speech + full MediaPipe video landmarks|
 */
enum class DeviceProfile {
    BASIC,
    STANDARD,
    ADVANCED
}

// ---------------------------------------------------------------------------
// Capability report
// ---------------------------------------------------------------------------

/**
 * Snapshot of the device's hardware capabilities and current runtime state.
 *
 * @param profile              Computed [DeviceProfile] tier.
 * @param apiLevel             [Build.VERSION.SDK_INT] at the time of assessment.
 * @param estimatedRamMb       Total physical RAM in megabytes (from ActivityManager).
 * @param availableStorageMb   Free storage on the app's data partition in megabytes.
 * @param hasNpu               True when a dedicated NPU/DSP is detected heuristically.
 * @param thermalStatus        [PowerManager] thermal status code (requires API 29+; -1 if unavailable).
 * @param batteryPct           Current battery level as a percentage (0â€“100; -1 if unavailable).
 * @param canRunWhisper        True when device meets Whisper Tiny requirements (â‰¥512MB RAM).
 * @param canRunMediaPipe      True when device meets MediaPipe requirements (â‰¥256MB RAM).
 * @param canRunConcurrently   True when device has â‰¥3GB RAM, allowing audio & video models in memory together.
 * @param canRunLocalSLM       Deprecated in Stage 1.2: always false (zero large LLMs).
 */
data class DeviceCapabilityReport(
    val profile: DeviceProfile,
    val apiLevel: Int,
    val estimatedRamMb: Long,
    val availableStorageMb: Long,
    val hasNpu: Boolean,
    val thermalStatus: Int,
    val batteryPct: Int,
    val canRunWhisper: Boolean,
    val canRunMediaPipe: Boolean,
    val canRunConcurrently: Boolean = false,
    @Deprecated("Removed in Stage 1.2 — no large LLMs used")
    val canRunLocalSLM: Boolean = false
)

// ---------------------------------------------------------------------------
// Manager
// ---------------------------------------------------------------------------

/**
 * Inspects device hardware to determine capability profile and enforce low-memory
 * protections (e.g. mutually exclusive model loading on devices with <3GB RAM).
 */
class DeviceCapabilityManager(private val context: Context? = null) {

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Performs a full device capability assessment and returns an immutable
     * [DeviceCapabilityReport] snapshot.
     */
    fun assess(): DeviceCapabilityReport {
        val ramMb = estimatedRamMb()
        val apiLevel = Build.VERSION.SDK_INT
        val storageMb = availableStorageMb()
        val thermalStatus = currentThermalStatus()
        val batteryPct = currentBatteryPct()
        val hasNpu = detectNpu()
        val profile = computeProfile(ramMb, apiLevel)

        val canRunMediaPipe = ramMb >= 256 && apiLevel >= 24
        val canRunWhisper = ramMb >= 512 && apiLevel >= 24
        val canRunConcurrently = ramMb >= 3072 && thermalStatus < 4 // Not thermally throttled

        return DeviceCapabilityReport(
            profile = profile,
            apiLevel = apiLevel,
            estimatedRamMb = ramMb,
            availableStorageMb = storageMb,
            hasNpu = hasNpu,
            thermalStatus = thermalStatus,
            batteryPct = batteryPct,
            canRunWhisper = canRunWhisper,
            canRunMediaPipe = canRunMediaPipe,
            canRunConcurrently = canRunConcurrently,
            canRunLocalSLM = false
        )
    }

    /**
     * Returns the [DeviceProfile] tier for the current device without building
     * the full report.
     */
    fun getProfile(): DeviceProfile = computeProfile(estimatedRamMb(), Build.VERSION.SDK_INT)

    /**
     * Returns true if audio and video perception models can safely run concurrently
     * without exceeding the device's heap or triggering OOM.
     */
    fun canRunAudioAndVideoConcurrently(): Boolean {
        return estimatedRamMb() >= 3072L
    }

    /**
     * Recommends video frame sampling interval in milliseconds based on device tier and thermals.
     * Prevents UI stutter on low-end hardware.
     */
    fun recommendedFrameSamplingIntervalMs(): Long {
        val thermal = currentThermalStatus()
        val ram = estimatedRamMb()
        return when {
            thermal >= 3 -> 2000L // 0.5 fps on hot phone
            ram < 2048L  -> 2000L // 0.5 fps on 2GB RAM phone
            ram < 4096L  -> 1000L // 1.0 fps on mid-range phone
            else         -> 1000L // 1.0 fps on high-end phone
        }
    }

    // -----------------------------------------------------------------------
    // Profile Computation
    // -----------------------------------------------------------------------

    fun computeProfile(ramMb: Long, apiLevel: Int): DeviceProfile {
        return when {
            ramMb < 2048 || apiLevel < 24 -> DeviceProfile.BASIC
            ramMb < 4096                  -> DeviceProfile.STANDARD
            else                          -> DeviceProfile.ADVANCED
        }
    }

    private fun estimatedRamMb(): Long {
        if (context == null) return 4096L
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem / (1024L * 1024L)
    }

    private fun availableStorageMb(): Long {
        if (context == null) return 8192L
        val statFs = StatFs(context.filesDir.absolutePath)
        val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong
        return availableBytes / (1024L * 1024L)
    }

    private fun currentThermalStatus(): Int {
        if (context == null) return -1
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.currentThermalStatus
        } else {
            -1
        }
    }

    private fun currentBatteryPct(): Int {
        if (context == null) return 100
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
        return if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        } else {
            -1
        }
    }

    private fun detectNpu(): Boolean {
        if (context == null) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.packageManager
            if (pm.hasSystemFeature("android.hardware.npu")) return true
        }
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            Build.HARDWARE
        }.lowercase()
        return soc.contains("tensor")
                || soc.contains("dimensity")
                || soc.contains("snapdragon")
                || soc.contains("kirin")
                || soc.contains("exynos")
    }
}

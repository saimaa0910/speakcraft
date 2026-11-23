package com.example.speech.metrics

/**
 * Target bands that turn a metric value into an issue (ARC-05 §6 band table), in each metric's band unit.
 *
 * An issue is raised only when the value is outside its band by more than the metric's noise band
 * (the `*_NOISE` constants), so a value just over the line is not called a mistake (ARC-05 §8.4).
 * A technique or activity may move one side of a band with a `REBAND` issue override (ARC-03 §6.8,
 * consistency item CL-60); the metric value and its label never change, only the interpretation.
 *
 * Units: words per minute; long pauses per minute of speaking (M-PAUSE is stored as a count, the
 * rate is derived for bands); fillers per minute; hedges per 100 words; MATTR 0–1.
 */
data class TargetBands(
    val wpmMin: Double = 100.0,
    val wpmMax: Double = 160.0,
    val pausesPerMinMax: Double = 2.0,
    val fillersPerMinMax: Double = 3.0,
    val hedgesPer100Max: Double = 3.0,
    val mattrMin: Double = 0.55
) {
    init {
        require(wpmMin < wpmMax) { "wpmMin ($wpmMin) must be below wpmMax ($wpmMax)" }
    }

    companion object {
        val DEFAULT = TargetBands()

        const val WPM_NOISE = 10.0
        const val PAUSE_NOISE = 1.0
        const val FILL_NOISE = 1.0
        const val HEDGE_NOISE = 1.0
        const val MATTR_NOISE = 0.05
    }
}

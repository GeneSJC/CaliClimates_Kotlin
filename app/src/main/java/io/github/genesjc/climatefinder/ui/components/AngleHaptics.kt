package io.github.genesjc.climatefinder.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect

/**
 * The feel of the viewing-angle slider, through the phone's vibrator directly like [RegionHaptics] (the view haptics are faint
 * and depend on the touch-feedback setting): a heavy click when the slider reaches 0, 45 or 90, and a light tick at the other
 * multiples of 15 degrees. Android 10 and later use the system's own effects; older versions a short pulse.
 */
object AngleHaptics {
    fun detent(context: Context) = play(context, VibrationEffect.EFFECT_HEAVY_CLICK, DETENT_MS, DETENT_AMPLITUDE)

    fun light(context: Context) = play(context, VibrationEffect.EFFECT_TICK, LIGHT_MS, LIGHT_AMPLITUDE)

    private fun play(context: Context, effect: Int, pulseMs: Long, amplitude: Int) {
        val vibrator = RegionHaptics.vibratorOf(context) ?: return
        if (!vibrator.hasVibrator()) return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> vibrator.vibrate(VibrationEffect.createPredefined(effect))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrate(VibrationEffect.createOneShot(pulseMs, amplitude))
            else -> @Suppress("DEPRECATION") vibrator.vibrate(pulseMs)
        }
    }

    private const val DETENT_MS = 35L
    private const val DETENT_AMPLITUDE = 220
    private const val LIGHT_MS = 12L
    private const val LIGHT_AMPLITUDE = 90
}

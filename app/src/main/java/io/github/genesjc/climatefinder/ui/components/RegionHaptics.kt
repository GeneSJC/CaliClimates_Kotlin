package io.github.genesjc.climatefinder.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The tick felt when the region changes. It uses the phone's vibrator directly, with the system's own "click" effect
 * (Android 10 and later) or a short pulse on older versions, so it is clearly felt and does not depend on the
 * touch-feedback setting the way the view haptics do. It is a click, not a buzz. Needs the VIBRATE permission, which
 * Android grants without asking.
 */
object RegionHaptics {
    fun tick(context: Context) {
        val vibrator = vibratorOf(context) ?: return
        if (!vibrator.hasVibrator()) return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                vibrator.vibrate(VibrationEffect.createOneShot(PULSE_MS, PULSE_AMPLITUDE))
            else -> @Suppress("DEPRECATION") vibrator.vibrate(PULSE_MS)
        }
    }

    internal fun vibratorOf(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        }

    private const val PULSE_MS = 25L
    private const val PULSE_AMPLITUDE = 140
}


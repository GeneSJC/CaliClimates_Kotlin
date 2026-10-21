package io.github.genesjc.microclimatefinder.data.local

import android.content.Context

/** Where the "tap the line" hint's memory is kept (a seam so tests can use a fake). */
interface ElevationHintStore {
    fun countLaunch()

    fun shouldShow(): Boolean

    fun markLineTapped()
}

/**
 * Remembers how many app launches have opened the Elevation view, and whether the user has already tapped
 * the line. The "tap the line" hint shows on each of the first few launches, and stops at the first tap.
 */
class ElevationHintPreferences(context: Context) : ElevationHintStore {

    private val prefs = context.getSharedPreferences("elevation_hint_prefs", Context.MODE_PRIVATE)

    // One instance lives for the whole app run, so this is true once this launch has been counted
    private var countedThisLaunch = false

    /** Counts this launch once, however many times the view is opened during it. */
    override fun countLaunch() {
        if (countedThisLaunch) return
        countedThisLaunch = true
        prefs.edit().putInt(KEY_LAUNCHES, prefs.getInt(KEY_LAUNCHES, 0) + 1).apply()
    }

    override fun shouldShow(): Boolean = !prefs.getBoolean(KEY_TAPPED, false) && prefs.getInt(KEY_LAUNCHES, 0) <= MAX_LAUNCHES

    override fun markLineTapped() {
        if (!prefs.getBoolean(KEY_TAPPED, false)) prefs.edit().putBoolean(KEY_TAPPED, true).apply()
    }

    private companion object {
        const val KEY_LAUNCHES = "launches"
        const val KEY_TAPPED = "line_tapped"
        const val MAX_LAUNCHES = 3
    }
}

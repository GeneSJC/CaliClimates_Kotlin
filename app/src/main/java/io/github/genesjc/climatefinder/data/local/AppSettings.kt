package io.github.genesjc.climatefinder.data.local

import android.content.Context
import android.content.SharedPreferences
import io.github.genesjc.climatefinder.domain.model.StateUsaDefinition

/**
 * Everything the app remembers between launches, in one place (a seam so tests can use a fake): the chosen region, the
 * station and map zoom set in it, the dark look, and the memory of the Elevation "tap the line" hint. A value that
 * belongs to a region (the station, the zoom) is only given back for the region it was saved in.
 */
interface AppSettings {
    /** The saved region id, or the default when nothing valid is saved. */
    fun selectedRegionId(): String

    fun saveSelectedRegionId(regionId: String)

    /** The saved station id, only if it was saved for [regionId]; null otherwise. */
    fun selectedStationId(regionId: String): String?

    /** Remembers the station selected in [regionId]; null forgets it. */
    fun saveSelectedStationId(regionId: String, stationId: String?)

    /** The saved map zoom, only if it was saved for [regionId]; 1 (not zoomed) otherwise. */
    fun mapZoom(regionId: String): Float

    fun saveMapZoom(regionId: String, zoom: Float)

    /** Whether the user wants the dark look. It starts off (the light look). */
    fun darkMode(): Boolean

    fun saveDarkMode(dark: Boolean)

    /** Counts this launch once, however many times the Elevation view is opened during it. */
    fun countElevationHintLaunch()

    /** The "tap the line" hint shows on each of the first few launches, and stops at the first tap. */
    fun shouldShowElevationHint(): Boolean

    fun markElevationLineTapped()
}

/** [AppSettings] kept in one SharedPreferences file, with one key per value. */
class AppSettingsPreferences(
    private val prefs: SharedPreferences,
    private val stateUsa: StateUsaDefinition
) : AppSettings {

    constructor(context: Context, stateUsa: StateUsaDefinition) :
        this(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE), stateUsa)

    // One instance lives for the whole app run, so this is true once this launch has been counted
    private var countedThisLaunch = false

    override fun selectedRegionId(): String {
        val saved = prefs.getString(KEY_REGION, null)
        return if (stateUsa.regions.any { it.id == saved }) saved!! else stateUsa.defaultRegionId
    }

    override fun saveSelectedRegionId(regionId: String) {
        prefs.edit().putString(KEY_REGION, regionId).apply()
    }

    override fun selectedStationId(regionId: String): String? =
        prefs.getString(KEY_STATION_ID, null)?.takeIf { prefs.getString(KEY_STATION_REGION, null) == regionId }

    override fun saveSelectedStationId(regionId: String, stationId: String?) {
        prefs.edit().putString(KEY_STATION_REGION, regionId).putString(KEY_STATION_ID, stationId).apply()
    }

    override fun mapZoom(regionId: String): Float =
        if (prefs.getString(KEY_ZOOM_REGION, null) == regionId) prefs.getFloat(KEY_ZOOM, 1f) else 1f

    override fun saveMapZoom(regionId: String, zoom: Float) {
        prefs.edit().putString(KEY_ZOOM_REGION, regionId).putFloat(KEY_ZOOM, zoom).apply()
    }

    override fun darkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    override fun saveDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
    }

    override fun countElevationHintLaunch() {
        if (countedThisLaunch) return
        countedThisLaunch = true
        prefs.edit().putInt(KEY_HINT_LAUNCHES, prefs.getInt(KEY_HINT_LAUNCHES, 0) + 1).apply()
    }

    override fun shouldShowElevationHint(): Boolean =
        !prefs.getBoolean(KEY_HINT_TAPPED, false) && prefs.getInt(KEY_HINT_LAUNCHES, 0) <= MAX_HINT_LAUNCHES

    override fun markElevationLineTapped() {
        if (!prefs.getBoolean(KEY_HINT_TAPPED, false)) prefs.edit().putBoolean(KEY_HINT_TAPPED, true).apply()
    }

    companion object {
        const val FILE_NAME = "app_settings"
        const val MAX_HINT_LAUNCHES = 3

        private const val KEY_REGION = "selected_region"
        private const val KEY_STATION_REGION = "station_region"
        private const val KEY_STATION_ID = "station_id"
        private const val KEY_ZOOM_REGION = "zoom_region"
        private const val KEY_ZOOM = "zoom"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_HINT_LAUNCHES = "hint_launches"
        private const val KEY_HINT_TAPPED = "hint_line_tapped"
    }
}

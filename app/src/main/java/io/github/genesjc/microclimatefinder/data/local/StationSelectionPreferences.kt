package io.github.genesjc.microclimatefinder.data.local

import android.content.Context

/** Where the last selected station is kept between launches (a seam so tests can use a fake). */
interface SelectedStationStore {
    /** The saved station id, only if it was saved for [regionId]; null otherwise. */
    fun load(regionId: String): String?

    /** Remembers the station selected in [regionId]; null forgets it. */
    fun save(regionId: String, stationId: String?)
}

/** Remembers the station the user last picked and the region it belongs to, so the app reopens on it. */
class StationSelectionPreferences(context: Context) : SelectedStationStore {

    private val prefs = context.getSharedPreferences("station_selection_prefs", Context.MODE_PRIVATE)

    override fun load(regionId: String): String? =
        prefs.getString(KEY_STATION_ID, null)?.takeIf { prefs.getString(KEY_REGION_ID, null) == regionId }

    override fun save(regionId: String, stationId: String?) {
        prefs.edit().putString(KEY_REGION_ID, regionId).putString(KEY_STATION_ID, stationId).apply()
    }

    private companion object {
        const val KEY_REGION_ID = "region_id"
        const val KEY_STATION_ID = "station_id"
    }
}

/** Where the map zoom is kept between launches (a seam so tests can use a fake). */
interface MapZoomStore {
    /** The saved zoom, only if it was saved for [regionId]; 1 (not zoomed) otherwise. */
    fun load(regionId: String): Float

    fun save(regionId: String, zoom: Float)
}

/** Remembers the map zoom and the region it was set in, so the app reopens at the same zoom. */
class MapZoomPreferences(context: Context) : MapZoomStore {

    private val prefs = context.getSharedPreferences("map_zoom_prefs", Context.MODE_PRIVATE)

    override fun load(regionId: String): Float =
        if (prefs.getString(KEY_REGION_ID, null) == regionId) prefs.getFloat(KEY_ZOOM, 1f) else 1f

    override fun save(regionId: String, zoom: Float) {
        prefs.edit().putString(KEY_REGION_ID, regionId).putFloat(KEY_ZOOM, zoom).apply()
    }

    private companion object {
        const val KEY_REGION_ID = "region_id"
        const val KEY_ZOOM = "zoom"
    }
}

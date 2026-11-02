package io.github.genesjc.climatefinder.data.local

/**
 * What the app remembers about where the user is, between launches: the chosen region, and the station and map zoom set
 * in it. A value that belongs to a region (the station, the zoom) is only given back for the region it was saved in.
 */
interface RegionSettings {
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
}

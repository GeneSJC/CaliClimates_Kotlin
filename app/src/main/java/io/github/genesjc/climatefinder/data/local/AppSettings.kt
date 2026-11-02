package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.domain.BandAngle

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

    /** Whether the Elevation view shows its layers (the bands). It starts on: the bands are the view the app is built around, and the single line is one tap away. */
    fun elevationLayers(): Boolean

    fun saveElevationLayers(on: Boolean)

    /**
     * The direction the Elevation Bands look across the region, in whole degrees from north (0, the start, as the bands
     * always ran) toward west (90); see [BandAngle].
     */
    fun elevationBandAngle(): Int

    fun saveElevationBandAngle(degrees: Int)
}


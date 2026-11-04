package io.github.genesjc.climatefinder.config

import androidx.annotation.StringRes

/**
 * One regional section of a USA state, for microclimate transects and geographical constraint definition.
 * Used to partition a state into distinct atmospheric and topoclimatic zones. The wording (names, titles,
 * descriptions, traits) is held as string resource ids; the key cities are place names and stay as data.
 */
data class StateUsaRegion(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val centerLat: Double,
    val centerLon: Double,
    val minElevationMeters: Int,
    val maxElevationMeters: Int,
    val keyCities: List<String>,
    val microclimateTraitRes: List<Int>,
    @StringRes val descriptionRes: Int,
    val status: RegionStatus,
    val activeStationCount: Int = 0,
    val colorHex: String,
    /** A shorter name for the Home header's folder tab, where it has little room; 0 means use [nameRes]. */
    @StringRes val headerNameRes: Int = 0
)


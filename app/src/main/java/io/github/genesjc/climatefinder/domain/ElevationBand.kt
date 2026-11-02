package io.github.genesjc.climatefinder.domain

import androidx.annotation.StringRes
import io.github.genesjc.climatefinder.R

/** The plain-language band for a height. Bands are by feet: 100, 500, 1,500, 3,500 and 6,000. */
enum class ElevationBand(@StringRes val labelRes: Int) {
    NEAR_SEA_LEVEL(R.string.elevation_band_near_sea_level),
    LOWLANDS(R.string.elevation_band_lowlands),
    FOOTHILLS(R.string.elevation_band_foothills),
    HILLS(R.string.elevation_band_hills),
    MOUNTAINS(R.string.elevation_band_mountains),
    HIGH_COUNTRY(R.string.elevation_band_high_country)
}

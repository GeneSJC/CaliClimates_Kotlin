package io.github.genesjc.climatefinder.ui.text

import androidx.annotation.StringRes
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.ElevationBand
import io.github.genesjc.climatefinder.domain.model.TerrainType

/**
 * The string resource that words each domain value. The domain enums are plain values; their wording is the UI's
 * business, and the exhaustive `when` makes a new value fail the build until it has words.
 */
@get:StringRes
val TerrainType.labelRes: Int
    get() = when (this) {
        TerrainType.COASTAL -> R.string.terrain_type_coastal
        TerrainType.RIDGE -> R.string.terrain_type_ridge
        TerrainType.VALLEY_BASIN -> R.string.terrain_type_valley_basin
        TerrainType.URBAN_CANYON -> R.string.terrain_type_urban_canyon
    }

@get:StringRes
val ElevationBand.labelRes: Int
    get() = when (this) {
        ElevationBand.NEAR_SEA_LEVEL -> R.string.elevation_band_near_sea_level
        ElevationBand.LOWLANDS -> R.string.elevation_band_lowlands
        ElevationBand.FOOTHILLS -> R.string.elevation_band_foothills
        ElevationBand.HILLS -> R.string.elevation_band_hills
        ElevationBand.MOUNTAINS -> R.string.elevation_band_mountains
        ElevationBand.HIGH_COUNTRY -> R.string.elevation_band_high_country
    }

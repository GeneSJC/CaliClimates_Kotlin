package io.github.genesjc.climatefinder.domain.model

import androidx.annotation.StringRes
import io.github.genesjc.climatefinder.R

enum class TerrainType(@StringRes val labelRes: Int) {
    COASTAL(R.string.terrain_type_coastal),
    RIDGE(R.string.terrain_type_ridge),
    VALLEY_BASIN(R.string.terrain_type_valley_basin),
    URBAN_CANYON(R.string.terrain_type_urban_canyon)
}

package io.github.genesjc.microclimatefinder.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.genesjc.microclimatefinder.R
import io.github.genesjc.microclimatefinder.domain.model.TerrainType

/** A general sentence about a station of this terrain type and height, for a station whose data has no note of its own. */
@Composable
fun terrainDescriptionText(terrain: TerrainType, elevationMeters: Int): String {
    val height = elevationFeetText(elevationMeters)
    return when (terrain) {
        TerrainType.RIDGE -> stringResource(R.string.terrain_description_ridge, height)
        TerrainType.COASTAL -> stringResource(R.string.terrain_description_coastal, height)
        TerrainType.URBAN_CANYON -> stringResource(R.string.terrain_description_urban, height)
        TerrainType.VALLEY_BASIN -> stringResource(R.string.terrain_description_valley, height)
    }
}

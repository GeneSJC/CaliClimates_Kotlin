package io.github.genesjc.climatefinder.ui.text

import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.ElevationBand
import io.github.genesjc.climatefinder.domain.model.TerrainType
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceLabelsTest {
    @Test
    fun terrainTypesHaveTheirOwnWording() {
        assertEquals(R.string.terrain_type_coastal, TerrainType.COASTAL.labelRes)
        assertEquals(R.string.terrain_type_ridge, TerrainType.RIDGE.labelRes)
        assertEquals(R.string.terrain_type_valley_basin, TerrainType.VALLEY_BASIN.labelRes)
        assertEquals(R.string.terrain_type_urban_canyon, TerrainType.URBAN_CANYON.labelRes)
    }

    @Test
    fun everyElevationBandHasItsOwnWording() {
        val ids = ElevationBand.values().map { it.labelRes }
        assertEquals(ids.size, ids.toSet().size)
    }
}

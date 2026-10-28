package io.github.genesjc.climatefinder.domain.model

import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.testStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroclimateStationTest {
    @Test
    fun aStationCanHaveALiveReadingUnlessTheDataSaysItHasNoTemperature() {
        assertTrue(testStation(reportsTemp = true).canHaveLiveReading)
        assertTrue(testStation(reportsTemp = null).canHaveLiveReading)
        assertFalse(testStation(reportsTemp = false).canHaveLiveReading)
    }

    @Test
    fun aReadingConvertsToFahrenheit() {
        val reading = testStation().currentReading
        assertEquals(68.0, reading.temperatureFahrenheit, 1e-9)
    }

    @Test
    fun terrainTypesHaveTheirOwnWording() {
        assertEquals(R.string.terrain_type_coastal, TerrainType.COASTAL.labelRes)
        assertEquals(R.string.terrain_type_ridge, TerrainType.RIDGE.labelRes)
        assertEquals(R.string.terrain_type_valley_basin, TerrainType.VALLEY_BASIN.labelRes)
        assertEquals(R.string.terrain_type_urban_canyon, TerrainType.URBAN_CANYON.labelRes)
    }
}

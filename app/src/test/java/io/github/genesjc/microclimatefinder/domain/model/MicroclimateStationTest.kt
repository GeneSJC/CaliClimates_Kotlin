package io.github.genesjc.microclimatefinder.domain.model

import io.github.genesjc.microclimatefinder.testStation
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
    fun terrainTypesHaveTheirDisplayNames() {
        assertEquals("Coastal Zone", TerrainType.COASTAL.label)
        assertEquals("Mountain Ridge", TerrainType.RIDGE.label)
        assertEquals("Valley Basin", TerrainType.VALLEY_BASIN.label)
        assertEquals("Urban Corridor", TerrainType.URBAN_CANYON.label)
    }
}

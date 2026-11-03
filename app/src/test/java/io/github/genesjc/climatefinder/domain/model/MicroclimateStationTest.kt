package io.github.genesjc.climatefinder.domain.model

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
    fun theDisplayNameIsTheTownAndTheStationsOwnNameWhereTheyDiffer() {
        assertEquals("Jackson (Mount Zion)", testStation(name = "Mount Zion", town = "Jackson").displayName)
    }

    @Test
    fun aNameThatAlreadySaysItsTownIsKept() {
        assertEquals("Sacramento Executive Airport", testStation(name = "Sacramento Executive Airport", town = "Sacramento").displayName)
    }

    @Test
    fun withoutATownTheDisplayNameIsTheName() {
        assertEquals("Rose Peak", testStation(name = "Rose Peak").displayName)
    }
}

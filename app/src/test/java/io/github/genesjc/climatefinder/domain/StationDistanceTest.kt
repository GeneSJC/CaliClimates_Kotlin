package io.github.genesjc.climatefinder.domain

import io.github.genesjc.climatefinder.testStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StationDistanceTest {
    // Santa Cruz (DeLaveaga) and Auburn airport, from the station file
    private val santaCruz = 36.9917 to -122.0083
    private val auburn = 38.9554 to -121.0864

    @Test
    fun sameSpotIsZeroMiles() {
        assertEquals(0.0, StationDistance.miles(38.0, -121.0, 38.0, -121.0), 0.0001)
    }

    @Test
    fun santaCruzToAuburnIsAboutOneHundredSixtyMiles() {
        val miles = StationDistance.miles(santaCruz.first, santaCruz.second, auburn.first, auburn.second)
        assertEquals(159.0, miles, 3.0)
    }

    @Test
    fun distanceIsTheSameBothWays() {
        val there = StationDistance.miles(santaCruz.first, santaCruz.second, auburn.first, auburn.second)
        val back = StationDistance.miles(auburn.first, auburn.second, santaCruz.first, santaCruz.second)
        assertEquals(there, back, 0.0001)
    }

    @Test
    fun oneDegreeOfLatitudeIsAboutSixtyNineMiles() {
        assertEquals(69.1, StationDistance.miles(37.0, -122.0, 38.0, -122.0), 0.3)
    }

    @Test
    fun depthIsTheShareOfTheSpanAndStaysBetweenZeroAndOne() {
        assertEquals(0f, StationDistance.depth(0.0, 100.0), 0f)
        assertEquals(0.25f, StationDistance.depth(25.0, 100.0), 0.0001f)
        assertEquals(1f, StationDistance.depth(180.0, 100.0), 0f)
    }

    @Test
    fun aRegionWithNoSpanHasNoDepth() {
        assertEquals(0f, StationDistance.depth(10.0, 0.0), 0f)
        assertFalse(StationDistance.depth(10.0, 0.0).isNaN())
    }

    @Test
    fun milesFromLeavesOutTheChosenStationAndRoundsToWholeMiles() {
        val a = testStation(id = "A", latitude = santaCruz.first, longitude = santaCruz.second)
        val b = testStation(id = "B", latitude = auburn.first, longitude = auburn.second)
        val miles = StationDistance.milesFrom(a, listOf(a, b))
        assertEquals(setOf("B"), miles.keys)
        assertEquals(159.0, miles.getValue("B").toDouble(), 3.0)
    }

    @Test
    fun spanIsTheCornerToCornerDistanceOfTheStations() {
        val a = testStation(id = "A", latitude = 37.0, longitude = -122.0)
        val b = testStation(id = "B", latitude = 38.0, longitude = -122.0)
        assertEquals(69.1, StationDistance.spanMiles(listOf(a, b)), 0.3)
        assertEquals(0.0, StationDistance.spanMiles(listOf(a)), 0.0)
    }
}

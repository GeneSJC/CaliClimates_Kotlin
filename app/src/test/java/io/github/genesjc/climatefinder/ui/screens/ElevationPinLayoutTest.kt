package io.github.genesjc.climatefinder.ui.screens

import io.github.genesjc.climatefinder.testStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevationPinLayoutTest {

    // San Jose (west, south), Sacramento (east, north), Gilroy (between, south), in an order unrelated to their places
    private val stations = listOf(
        testStation(id = "KSMF", latitude = 38.58, longitude = -121.49),
        testStation(id = "KRHV", latitude = 37.33, longitude = -121.82),
        testStation(id = "GYYC1", latitude = 37.0, longitude = -121.57)
    )

    @Test
    fun lookingNorthTheStationsStandWestToEast() {
        val at = lateralPositions(stations, 0)
        // San Jose is the westernmost, then Gilroy, then Sacramento
        assertTrue(at[1] < at[2] && at[2] < at[0])
        assertEquals(0f, at.min(), 0.0001f)
        assertEquals(1f, at.max(), 0.0001f)
    }

    @Test
    fun lookingWestTheStationsStandSouthToNorth() {
        val at = lateralPositions(stations, 90)
        // Gilroy is the southernmost, then San Jose, then Sacramento
        assertTrue(at[2] < at[1] && at[1] < at[0])
    }

    @Test
    fun noStationsMeansNoPositions() {
        assertEquals(0, lateralPositions(emptyList(), 0).size)
    }

    @Test
    fun aSingleStationStandsAtTheStart() {
        assertEquals(0f, lateralPositions(stations.take(1), 0)[0], 0f)
    }

    @Test
    fun theSingleLinePutsStationsWhereTheyRankByHeight() {
        val lateral = lateralPositions(stations, 0)
        val first = elevationPinX(0, 3, 0f, lateral, paddingPx = 10f, widthPx = 200f)
        val last = elevationPinX(2, 3, 0f, lateral, paddingPx = 10f, widthPx = 200f)
        assertEquals(10f, first, 0.0001f)
        assertEquals(210f, last, 0.0001f)
    }

    @Test
    fun withTheBandsOnStationsStandWhereTheyAreAcrossTheView() {
        val lateral = lateralPositions(stations, 0)
        val x = elevationPinX(0, 3, 1f, lateral, paddingPx = 10f, widthPx = 200f)
        assertEquals(10f + lateral[0] * 200f, x, 0.0001f)
    }

    @Test
    fun halfwayThroughTheFoldAStationIsHalfwayBetween() {
        val lateral = lateralPositions(stations, 0)
        val single = elevationPinX(0, 3, 0f, lateral, 10f, 200f)
        val bands = elevationPinX(0, 3, 1f, lateral, 10f, 200f)
        assertEquals((single + bands) / 2f, elevationPinX(0, 3, 0.5f, lateral, 10f, 200f), 0.0001f)
    }

    @Test
    fun theSingleLineKeepsTheHeightProfileWhateverTheBandAndAngle() {
        val a = elevationPinY(500, 1000f, band = 0, lateral = 0.2f, mix = 0f, angleShown = 0, paddingPx = 10f, widthPx = 300f, heightPx = 400f)
        val b = elevationPinY(500, 1000f, band = 2, lateral = 0.9f, mix = 0f, angleShown = 60, paddingPx = 10f, widthPx = 300f, heightPx = 400f)
        assertEquals(a, b, 0.0001f)
    }

    @Test
    fun withTheBandsOnTheBackBandStandsHigherThanTheFront() {
        val front = elevationPinY(500, 1000f, band = 0, lateral = 0.5f, mix = 1f, angleShown = 0, paddingPx = 10f, widthPx = 300f, heightPx = 400f)
        val back = elevationPinY(500, 1000f, band = 2, lateral = 0.5f, mix = 1f, angleShown = 0, paddingPx = 10f, widthPx = 300f, heightPx = 400f)
        assertTrue("the back band is nearer the top (a smaller y)", back < front)
    }

    @Test
    fun aTallerStationStandsHigherOnTheChart() {
        val low = elevationPinY(100, 1000f, band = 1, lateral = 0.5f, mix = 1f, angleShown = 0, paddingPx = 10f, widthPx = 300f, heightPx = 400f)
        val high = elevationPinY(900, 1000f, band = 1, lateral = 0.5f, mix = 1f, angleShown = 0, paddingPx = 10f, widthPx = 300f, heightPx = 400f)
        assertTrue(high < low)
    }
}

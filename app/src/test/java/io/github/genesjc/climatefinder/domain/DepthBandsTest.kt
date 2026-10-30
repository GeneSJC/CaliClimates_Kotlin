package io.github.genesjc.climatefinder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthBandsTest {

    /** Six real stations of the Bay Area and Sacramento pack: id, latitude, longitude. */
    private val stations = listOf(
        Triple("Lincoln", 38.9092, -121.3513),
        Triple("San Jose Reid-Hillview", 37.3333, -121.8167),
        Triple("Half Moon Bay", 37.5136, -122.4996),
        Triple("Modesto", 37.6255, -120.9549),
        Triple("Oakland", 37.7213, -122.2211),
        Triple("Stockton", 37.8897, -121.2236)
    )

    private fun bandsAt(axis: BandAxis) = DepthBands.byRank(stations.map { axis.depthKey(it.second, it.third) }).toList()

    @Test
    fun theTwoAxesAreAtZeroAndNinetyDegrees() {
        assertEquals(0.0, BandAxis.NORTH_SOUTH.degrees, 0.0)
        assertEquals(90.0, BandAxis.WEST_EAST.degrees, 0.0)
    }

    @Test
    fun otherFlipsTheAxis() {
        assertEquals(BandAxis.WEST_EAST, BandAxis.NORTH_SOUTH.other())
        assertEquals(BandAxis.NORTH_SOUTH, BandAxis.WEST_EAST.other())
    }

    @Test
    fun theKeyIsThePlainLatitudeOrMinusLongitudeAtTheTwoAxes() {
        assertEquals(38.9092, BandAxis.NORTH_SOUTH.depthKey(38.9092, -121.3513), 0.0)
        assertEquals(121.3513, BandAxis.WEST_EAST.depthKey(38.9092, -121.3513), 0.0)
        assertEquals(38.9092, DepthBands.depthKey(38.9092, -121.3513, 0.0), 0.0)
        assertEquals(121.3513, DepthBands.depthKey(38.9092, -121.3513, 90.0), 0.0)
    }

    @Test
    fun aBiggerKeyIsFartherBack() {
        // North is farther back looking north; west (the more negative longitude) is farther back looking west
        assertTrue(BandAxis.NORTH_SOUTH.depthKey(39.0, -121.0) > BandAxis.NORTH_SOUTH.depthKey(37.0, -121.0))
        assertTrue(BandAxis.WEST_EAST.depthKey(38.0, -122.5) > BandAxis.WEST_EAST.depthKey(38.0, -120.9))
    }

    @Test
    fun westIsAtTheBackAndEastInFront() {
        val bands = DepthBands.byRank(listOf(-122.4, -120.9, -121.7).map { BandAxis.WEST_EAST.depthKey(38.0, it) })
        assertEquals(listOf(2, 0, 1), bands.toList())
    }

    @Test
    fun theTwoAxesGroupRealStationsDifferently() {
        // Lincoln, San Jose, Half Moon Bay, Modesto, Oakland, Stockton
        assertEquals(listOf(2, 0, 0, 1, 1, 2), bandsAt(BandAxis.NORTH_SOUTH))
        assertEquals(listOf(1, 1, 2, 0, 2, 0), bandsAt(BandAxis.WEST_EAST))
        assertNotEquals(bandsAt(BandAxis.NORTH_SOUTH), bandsAt(BandAxis.WEST_EAST))
    }

    @Test
    fun neighboursOnOneAxisCanFallApartOnTheOther() {
        val north = bandsAt(BandAxis.NORTH_SOUTH)
        val west = bandsAt(BandAxis.WEST_EAST)
        // Lincoln and San Jose are far apart north to south, but in the same band west to east
        assertNotEquals(north[0], north[1])
        assertEquals(west[0], west[1])
        // Half Moon Bay is at the back and Modesto in front when looking west
        assertEquals(2, west[2])
        assertEquals(0, west[3])
    }

    @Test
    fun aDiagonalKeyGroupsRealStationsBetweenTheTwoAxes() {
        val bands = DepthBands.byRank(stations.map { DepthBands.depthKey(it.second, it.third, 45.0) }).toList()
        assertEquals(listOf(2, 1, 2, 0, 1, 0), bands)
    }

    @Test
    fun bothAxesGiveBandsOfTheSameSize() {
        val keys = { axis: BandAxis -> List(30) { axis.depthKey(36.0 + it * 0.1, -124.0 + it * 0.07) } }
        for (axis in BandAxis.entries) {
            val bands = DepthBands.byRank(keys(axis))
            assertEquals(listOf(10, 10, 10), (0 until DepthBands.COUNT).map { band -> bands.count { it == band } })
        }
    }

    @Test
    fun theOrderOfTheStationsDoesNotChangeTheirBands() {
        for (axis in BandAxis.entries) {
            val forward = stations.map { axis.depthKey(it.second, it.third) }
            val backward = forward.reversed()
            assertEquals(DepthBands.byRank(forward).toList(), DepthBands.byRank(backward).reversedArray().toList())
        }
    }

    @Test
    fun aHalfwayAngleGroupsBetweenTheTwoEnds() {
        val bands = { degrees: Double -> DepthBands.byRank(stations.map { DepthBands.depthKey(it.second, it.third, degrees) }).toList() }
        assertEquals(bandsAt(BandAxis.NORTH_SOUTH), bands(0.0))
        assertEquals(bandsAt(BandAxis.WEST_EAST), bands(90.0))
        assertEquals(listOf(2, 1, 2, 0, 1, 0), bands(45.0))
    }

    @Test
    fun southIsInFrontAndNorthAtTheBack() {
        // Auburn 38.96, Gilroy-ish 37.0, Sacramento 38.7, Palo Alto 37.46, Vacaville 38.38, Santa Cruz 36.99
        val bands = DepthBands.byRank(listOf(38.96, 37.0, 38.7, 37.46, 38.38, 36.99))
        assertEquals(listOf(2, 0, 2, 1, 1, 0), bands.toList())
    }

    @Test
    fun theBandsAreAboutTheSameSize() {
        val bands = DepthBands.byRank(List(30) { 36.0 + it * 0.1 })
        assertEquals(listOf(10, 10, 10), (0 until DepthBands.COUNT).map { band -> bands.count { it == band } })
    }

    @Test
    fun theOrderOfTheInputDoesNotMatter() {
        val north = DepthBands.byRank(listOf(39.0, 37.0, 38.0))
        assertEquals(listOf(2, 0, 1), north.toList())
    }

    @Test
    fun noStationsMeansNoBands() {
        assertEquals(0, DepthBands.byRank(emptyList()).size)
    }

    @Test
    fun theNearestBandIsThePlainProfile() {
        assertEquals(123f, DepthBands.layeredY(123f, 0, topY = 20f, seaY = 400f), 0.0001f)
        assertEquals(0f, DepthBands.haze(0), 0f)
        assertEquals(1f, DepthBands.lineFade(0), 0f)
    }

    @Test
    fun fartherBandsSitHigherAndFainter() {
        val y = 200f
        val ys = (0 until DepthBands.COUNT).map { DepthBands.layeredY(y, it, topY = 20f, seaY = 400f) }
        assertTrue(ys.zipWithNext().all { (a, b) -> b < a })
        val haze = (0 until DepthBands.COUNT).map(DepthBands::haze)
        assertTrue(haze.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun subduedLayersStayVisible() {
        assertTrue(DepthBands.INACTIVE_FADE in 0.3f..0.9f)
        assertTrue(DepthBands.INACTIVE_WASH in 0.1f..0.5f)
    }

    @Test
    fun theSummitOfEachBandStandsClearAboveTheOneInFront() {
        val top = 20f
        val sea = 400f
        val range = sea - top
        val summits = (0 until DepthBands.COUNT).map { DepthBands.layeredY(top, it, top, sea) }
        // Each band's summit is at least 5 percent of the range above the band in front's
        assertTrue(summits.zipWithNext().all { (front, back) -> front - back >= 0.05f * range })
    }

    @Test
    fun theTallestBandStaysInTheSkyAboveTheChartNotOffIt() {
        val top = 20f
        val sea = 400f
        val range = sea - top
        val highest = DepthBands.layeredY(top, DepthBands.COUNT - 1, top, sea)
        assertTrue(highest < top)
        assertTrue(top - highest <= 0.2f * range)
    }

    @Test
    fun aStationAtSeaLevelIsStillAboveSeaLevelInTheFarBands() {
        val ys = (1 until DepthBands.COUNT).map { DepthBands.layeredY(400f, it, 20f, 400f) }
        assertTrue(ys.all { it < 400f })
        assertTrue(ys.zipWithNext().all { (a, b) -> b < a })
    }
}

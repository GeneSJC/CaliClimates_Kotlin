package io.github.genesjc.climatefinder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthBandsTest {
    @Test
    fun southIsInFrontAndNorthAtTheBack() {
        // Auburn 38.96, Gilroy-ish 37.0, Sacramento 38.7, Palo Alto 37.46, Vacaville 38.38, Santa Cruz 36.99
        val bands = DepthBands.byLatitude(listOf(38.96, 37.0, 38.7, 37.46, 38.38, 36.99))
        assertEquals(listOf(2, 0, 2, 1, 1, 0), bands.toList())
    }

    @Test
    fun theBandsAreAboutTheSameSize() {
        val bands = DepthBands.byLatitude(List(30) { 36.0 + it * 0.1 })
        assertEquals(listOf(10, 10, 10), (0 until DepthBands.COUNT).map { band -> bands.count { it == band } })
    }

    @Test
    fun theOrderOfTheInputDoesNotMatter() {
        val north = DepthBands.byLatitude(listOf(39.0, 37.0, 38.0))
        assertEquals(listOf(2, 0, 1), north.toList())
    }

    @Test
    fun noStationsMeansNoBands() {
        assertEquals(0, DepthBands.byLatitude(emptyList()).size)
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
    fun noBandRisesAboveTheTopOfTheChart() {
        val top = 20f
        for (band in 0 until DepthBands.COUNT) {
            assertTrue(DepthBands.layeredY(top, band, top, seaY = 400f) >= top)
        }
    }
}

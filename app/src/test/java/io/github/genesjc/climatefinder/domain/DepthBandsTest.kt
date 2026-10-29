package io.github.genesjc.climatefinder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthBandsTest {
    @Test
    fun depthFallsIntoNearMiddleOrFar() {
        assertEquals(0, DepthBands.bandOf(0f))
        assertEquals(0, DepthBands.bandOf(0.19f))
        assertEquals(1, DepthBands.bandOf(0.2f))
        assertEquals(1, DepthBands.bandOf(0.39f))
        assertEquals(2, DepthBands.bandOf(0.4f))
        assertEquals(2, DepthBands.bandOf(1f))
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

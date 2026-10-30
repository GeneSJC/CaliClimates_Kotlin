package io.github.genesjc.climatefinder.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevationPanLimitTest {

    private val height = 1000f

    /** The share of the chart's height the sea takes with the chart panned by [panY] at [zoom]. */
    private fun waterShare(zoom: Float, panY: Float): Float {
        val heightPx = height - 2 * CHART_PADDING_PX
        val seaY = CHART_PADDING_PX + (1f - SEA_FRACTION) * heightPx
        val seaOnScreen = height / 2f + (seaY - height / 2f) * zoom + panY
        return (height - seaOnScreen) / height
    }

    @Test
    fun theStartingViewIsWithinTheLimit() {
        assertTrue(elevationLowestPanY(1f, height) < 0f)
        assertTrue(waterShare(1f, 0f) < MAX_WATER_SHARE)
    }

    @Test
    fun atTheLimitTheSeaTakesExactlyTheMaximumShare() {
        for (zoom in listOf(1f, 2f, 4f, 8f)) {
            assertEquals(MAX_WATER_SHARE, waterShare(zoom, elevationLowestPanY(zoom, height)), 1e-4f)
        }
    }

    @Test
    fun theLimitLoosensAsTheChartIsZoomedIn() {
        val lows = listOf(1f, 2f, 4f, 8f).map { elevationLowestPanY(it, height) }
        assertTrue(lows.zipWithNext().all { (a, b) -> b < a })
    }

    @Test
    fun aTallerChartAllowsMoreRoomInPixels() {
        assertTrue(elevationLowestPanY(1f, 2000f) < elevationLowestPanY(1f, 1000f))
    }
}

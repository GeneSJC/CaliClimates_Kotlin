package io.github.genesjc.climatefinder.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapProjectionTest {
    // A 2 degree by 2 degree frame around 37 N, 121 W
    private fun square(w: Float = 1000f, h: Float = 1000f, padding: Float = 0f) =
        MapProjection(-122.0, -120.0, 36.0, 38.0, w, h, padding)

    @Test
    fun northIsAtTheTopAndSouthAtTheBottom() {
        val p = square()
        assertEquals(0f, p.baseY(38.0), 0.01f)
        assertEquals(1000f, p.baseY(36.0), 0.01f)
        assertTrue(p.baseY(37.5) < p.baseY(36.5))
    }

    @Test
    fun theFrameIsCentredWhenTheCanvasIsWider() {
        val p = square(w = 2000f)
        assertEquals(1000f, p.baseX(-121.0), 0.01f)
        assertEquals(0f, p.baseY(38.0), 0.01f)
    }

    @Test
    fun theFrameIsCentredWhenTheCanvasIsTaller() {
        val p = square(h = 2000f)
        assertEquals(1000f, p.baseY(37.0), 0.01f)
        assertEquals(500f, p.baseX(-121.0), 0.01f)
    }

    @Test
    fun paddingInsetsTheFrame() {
        val p = square(padding = 50f)
        assertEquals(50f, p.baseY(38.0), 0.01f)
        assertEquals(950f, p.baseY(36.0), 0.01f)
    }

    @Test
    fun oneScaleInBothDirectionsKeepsTheTrueShape() {
        val p = square()
        val northSouth = p.baseY(36.0) - p.baseY(37.0)           // one degree of latitude
        val eastWest = p.baseX(-120.0) - p.baseX(-121.0)         // one degree of longitude, at 37 N
        assertEquals(Math.cos(Math.toRadians(37.0)).toFloat(), eastWest / northSouth, 0.001f)
    }

    @Test
    fun pixelsPerKilometreFollowsTheScale() {
        val p = square()
        // 1,000 px for 2 degrees of latitude = 500 px per degree = 500 / 111.19 per km
        assertEquals(500.0 / 111.19, p.pixelsPerKm, 1e-6)
    }
}

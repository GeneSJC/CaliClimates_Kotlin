package io.github.genesjc.climatefinder.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class MapZoomTest {

    private fun assertOffset(x: Float, y: Float, actual: Offset) {
        assertEquals("x", x, actual.x, 1e-3f)
        assertEquals("y", y, actual.y, 1e-3f)
    }

    @Test
    fun dotsGrowAQuarterPerZoomStepUpToDouble() {
        assertEquals(1f, zoomDotGrowth(1f), 1e-6f)
        assertEquals(1.25f, zoomDotGrowth(2f), 1e-6f)
        assertEquals(2f, zoomDotGrowth(5f), 1e-6f)
        assertEquals(2f, zoomDotGrowth(MAX_MAP_ZOOM), 1e-6f)
    }

    @Test
    fun theLabelBudgetGrowsWithZoom() {
        assertEquals(10f, labelBudget(10, 1f, 3f), 1e-6f)
        assertEquals(16f, labelBudget(10, 3f, 3f), 1e-6f)
    }

    @Test
    fun aVerticalFloorStopsTheContentBeingDraggedUpTooFar() {
        // Zoom 1, a viewport 1000 tall, 40% overscroll: the pan could go to -400, but the floor at this zoom is -100
        val floor = { _: Float -> -100f }
        assertOffset(0f, -100f, clampPan(Offset(0f, -300f), 1f, 1000f, 1000f, overscroll = 0.4f, lowestPanY = floor))
        assertOffset(0f, -60f, clampPan(Offset(0f, -60f), 1f, 1000f, 1000f, overscroll = 0.4f, lowestPanY = floor))
        assertOffset(0f, 400f, clampPan(Offset(0f, 700f), 1f, 1000f, 1000f, overscroll = 0.4f, lowestPanY = floor))
    }

    @Test
    fun theVerticalFloorNeverPassesTheOtherLimit() {
        assertEquals(40f, clampPan(Offset(0f, 0f), 1f, 100f, 100f, overscroll = 0.4f, lowestPanY = { 500f }).y, 1e-3f)
    }

    @Test
    fun panIsLockedWhenFullyZoomedOut() {
        assertOffset(0f, 0f, clampPan(Offset(80f, -40f), 1f, 100f, 200f))
    }

    @Test
    fun panStopsWhereTheZoomedContentMeetsTheEdge() {
        // At 2x the content is twice the viewport, so half a viewport of pan each way
        assertOffset(50f, -100f, clampPan(Offset(80f, -300f), 2f, 100f, 200f))
        assertOffset(20f, -30f, clampPan(Offset(20f, -30f), 2f, 100f, 200f))
    }

    @Test
    fun theInsetKeepsItsMarginFixed() {
        // An inset of 10 each side leaves 80 of content width: (2 - 1) * 80 / 2 = 40
        assertOffset(40f, 0f, clampPan(Offset(90f, 0f), 2f, 100f, 200f, insetX = 10f))
    }

    @Test
    fun overscrollAllowsAPullPastTheEdgeAtAnyZoom() {
        assertOffset(10f, 20f, clampPan(Offset(90f, 90f), 1f, 100f, 200f, overscroll = 0.1f))
    }

    @Test
    fun zoomingAroundTheCentreKeepsTheMapCentred() {
        assertOffset(0f, 0f, panForZoom(Offset.Zero, 1f, 2f, Offset(50f, 50f), 100f, 100f))
    }

    @Test
    fun zoomingKeepsThePointUnderTheFingerWhereItIs() {
        // Anchor 25 right of centre: doubling the zoom shifts the content left by 25 to hold it still
        assertOffset(-25f, 0f, panForZoom(Offset.Zero, 1f, 2f, Offset(75f, 50f), 100f, 100f))
    }

    @Test
    fun zoomingBackOutRecentresAndNeverExposesAnEdge() {
        // Back at 1x there is nothing to pan
        assertOffset(0f, 0f, panForZoom(Offset(-25f, 0f), 2f, 1f, Offset(75f, 50f), 100f, 100f))
    }
}

package io.github.genesjc.climatefinder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthCueTest {
    @Test
    fun nearIsLargerAndSolidFarIsSmallerAndFainter() {
        assertEquals(1.1f, DepthCue.textScale(0f), 0.0001f)
        assertEquals(0.8f, DepthCue.textScale(1f), 0.0001f)
        assertEquals(1f, DepthCue.alpha(0f), 0.0001f)
        assertEquals(0.6f, DepthCue.alpha(1f), 0.0001f)
        assertEquals(1f, DepthCue.dotScale(0f), 0.0001f)
        assertEquals(0.8f, DepthCue.dotScale(1f), 0.0001f)
    }

    @Test
    fun theFactorsFallAsTheStationGetsFarther() {
        val depths = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        assertTrue(depths.zipWithNext().all { (a, b) -> DepthCue.textScale(a) > DepthCue.textScale(b) })
        assertTrue(depths.zipWithNext().all { (a, b) -> DepthCue.alpha(a) > DepthCue.alpha(b) })
    }

    @Test
    fun depthsOutsideZeroToOneAreHeldAtTheEnds() {
        assertEquals(DepthCue.textScale(1f), DepthCue.textScale(3f), 0f)
        assertEquals(DepthCue.alpha(0f), DepthCue.alpha(-1f), 0f)
    }
}

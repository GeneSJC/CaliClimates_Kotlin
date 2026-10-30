package io.github.genesjc.climatefinder.ui.components

import io.github.genesjc.climatefinder.domain.BandAxis
import io.github.genesjc.climatefinder.domain.DepthBands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerColorsTest {

    @Test
    fun northToSouthUsesTheColoursInTheirOrder() {
        for (band in 0 until DepthBands.COUNT) {
            assertEquals(LayerColors[band], layerColor(band, BandAxis.NORTH_SOUTH))
        }
    }

    @Test
    fun westToEastUsesTheSameColoursInTheOppositeOrder() {
        for (band in 0 until DepthBands.COUNT) {
            assertEquals(LayerColors[DepthBands.COUNT - 1 - band], layerColor(band, BandAxis.WEST_EAST))
        }
    }

    @Test
    fun theFrontBandChangesColourWhenTheAxisChanges() {
        assertNotEquals(layerColor(0, BandAxis.NORTH_SOUTH), layerColor(0, BandAxis.WEST_EAST))
        assertEquals(layerColor(0, BandAxis.NORTH_SOUTH), layerColor(DepthBands.COUNT - 1, BandAxis.WEST_EAST))
    }

    @Test
    fun theLooksRunInOppositeOrderOnTheTwoAxes() {
        assertEquals(listOf(0, 1, 2), (0 until DepthBands.COUNT).map { bandLook(it, BandAxis.NORTH_SOUTH) })
        assertEquals(listOf(2, 1, 0), (0 until DepthBands.COUNT).map { bandLook(it, BandAxis.WEST_EAST) })
    }

    @Test
    fun theColourFollowsTheLook() {
        for (axis in BandAxis.entries) {
            for (band in 0 until DepthBands.COUNT) {
                assertEquals(LayerColors[bandLook(band, axis)], layerColor(band, axis))
            }
        }
    }

    @Test
    fun theMiddleBandKeepsItsColourOnBothAxes() {
        assertEquals(layerColor(1, BandAxis.NORTH_SOUTH), layerColor(1, BandAxis.WEST_EAST))
    }

    @Test
    fun everyAxisUsesAllThreeColours() {
        for (axis in BandAxis.entries) {
            assertEquals(3, (0 until DepthBands.COUNT).map { layerColor(it, axis) }.toSet().size)
        }
    }

    @Test
    fun theColourAtTheEndsIsTheAxisColour() {
        for (band in 0 until DepthBands.COUNT) {
            assertEquals(layerColor(band, BandAxis.NORTH_SOUTH), layerColor(band, 0))
            assertEquals(layerColor(band, BandAxis.WEST_EAST), layerColor(band, 90))
        }
    }

    @Test
    fun theMiddleBandIsTealAtEveryAngle() {
        for (angle in 0..90 step 15) assertEquals(LayerColors[1], layerColor(1, angle))
    }

    @Test
    fun theOuterBandsBlendBetweenTheEnds() {
        val half = layerColor(0, 45)
        assertNotEquals(layerColor(0, 0), half)
        assertNotEquals(layerColor(0, 90), half)
        assertNotEquals(layerColor(0, 20), layerColor(0, 60))
    }

    @Test
    fun theKeyAndHazeFollowTheEndsToo() {
        for (band in 0 until DepthBands.COUNT) {
            assertEquals(LayerKeyColors[bandLook(band, BandAxis.NORTH_SOUTH)], keyColor(band, 0))
            assertEquals(LayerKeyColors[bandLook(band, BandAxis.WEST_EAST)], keyColor(band, 90))
            assertEquals(DepthBands.haze(bandLook(band, BandAxis.NORTH_SOUTH)), hazeColor(band, 0).alpha, 0.001f)
            assertEquals(DepthBands.haze(bandLook(band, BandAxis.WEST_EAST)), hazeColor(band, 90).alpha, 0.001f)
        }
    }

    @Test
    fun theFrontHazeGrowsAsTheAngleTurnsWest() {
        assertEquals(0f, hazeColor(0, 0).alpha, 0f)
        assertTrue(hazeColor(0, 45).alpha > 0f)
        assertTrue(hazeColor(0, 90).alpha > hazeColor(0, 45).alpha)
    }
}

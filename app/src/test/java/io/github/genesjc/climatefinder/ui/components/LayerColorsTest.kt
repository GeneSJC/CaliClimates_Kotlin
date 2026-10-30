package io.github.genesjc.climatefinder.ui.components

import io.github.genesjc.climatefinder.domain.BandAxis
import io.github.genesjc.climatefinder.domain.DepthBands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}

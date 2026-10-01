package io.github.genesjc.microclimatefinder.ui.components

import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The region outlines drawn in the California picker icon. */
class CaliforniaRegionShapesTest {

    @Test
    fun everyRegionHasAShapeAndNothingElseDoes() {
        assertEquals(
            CaliforniaRegionalDefinitions.ALL_REGIONS.map { it.id }.toSet(),
            CALIFORNIA_REGION_SHAPES.keys
        )
    }

    @Test
    fun everyShapeIsAClosablePolygonInsideCalifornia() {
        for ((id, points) in CALIFORNIA_REGION_SHAPES) {
            assertTrue("$id has ${points.size} points", points.size >= 3)
            for ((lon, lat) in points) {
                assertTrue("$id lon $lon", lon in -124.6f..-114.0f)
                assertTrue("$id lat $lat", lat in 32.0f..42.1f)
            }
        }
    }
}

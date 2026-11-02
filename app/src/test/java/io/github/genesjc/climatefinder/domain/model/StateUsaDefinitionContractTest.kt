package io.github.genesjc.climatefinder.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.genesjc.climatefinder.config.StateUsaCA
import io.github.genesjc.climatefinder.config.StateUsaDefinition

/**
 * What every USA state definition must satisfy, whatever the state: each fact the app looks up by region id covers
 * exactly the state's regions, and the picker map's artwork stays inside its window. Add a new state's object to
 * [definitions] and it is held to the same rules.
 */
class StateUsaDefinitionContractTest {

    private val definitions: List<StateUsaDefinition> = listOf(StateUsaCA)

    private fun eachDefinition(check: (StateUsaDefinition, Set<String>) -> Unit) {
        for (d in definitions) check(d, d.regions.map { it.id }.toSet())
    }

    private fun inside(view: GeoBounds, lon: Double, lat: Double) =
        lon in view.minLon..view.maxLon && lat in view.minLat..view.maxLat

    @Test
    fun hasACodeAndRegionsWithUniqueIds() = eachDefinition { d, ids ->
        assertTrue("${d.code} code", d.code.isNotBlank())
        assertTrue("${d.code} regions", d.regions.isNotEmpty())
        assertEquals("${d.code} unique ids", d.regions.size, ids.size)
    }

    @Test
    fun theDefaultRegionIsOneOfTheRegions() = eachDefinition { d, ids ->
        assertTrue("${d.code} default ${d.defaultRegionId}", d.defaultRegionId in ids)
        assertEquals(d.defaultRegionId, d.defaultRegion().id)
    }

    @Test
    fun findsARegionByIdAndNullForAnyOtherId() = eachDefinition { d, _ ->
        for (r in d.regions) assertEquals(r, d.region(r.id))
        assertEquals(null, d.region("atlantis"))
    }

    @Test
    fun theStateWordingIsFilledIn() = eachDefinition { d, _ ->
        assertTrue("${d.code} tagline", d.taglineRes != 0)
        assertTrue("${d.code} screen title", d.regionsScreenTitleRes != 0)
        assertTrue("${d.code} map title", d.regionsMapTitleRes != 0)
    }

    @Test
    fun everyRegionHasAShapeAndNothingElseDoes() = eachDefinition { d, ids ->
        assertEquals("${d.code} shapes", ids, d.regionShapes.keys)
    }

    @Test
    fun everyShapeIsAClosablePolygonInsideTheMapWindow() = eachDefinition { d, _ ->
        for ((id, points) in d.regionShapes) {
            assertTrue("${d.code} $id has ${points.size} points", points.size >= 3)
            for ((lon, lat) in points) {
                assertTrue("${d.code} $id point $lon, $lat", inside(d.pickerMap.view, lon.toDouble(), lat.toDouble()))
            }
        }
    }

    @Test
    fun theLookupsByRegionCoverExactlyTheRegions() = eachDefinition { d, ids ->
        assertEquals("${d.code} station packs", ids, d.stationPacks.keys)
        assertEquals("${d.code} terrain bounds", ids, d.terrainBounds.keys)
        assertEquals("${d.code} landmarks", ids, d.landmarks.keys)
        assertEquals("${d.code} tree mix", ids, d.treeMix.keys)
        assertEquals("${d.code} pack files are distinct", ids.size, d.stationPacks.values.toSet().size)
        for (id in ids) {
            assertTrue("${d.code} $id landmarks", d.landmarks.getValue(id).isNotEmpty())
            assertTrue("${d.code} $id trees", d.treeMix.getValue(id).isNotEmpty())
        }
    }

    @Test
    fun everyStationTownEntryIsFilledIn() = eachDefinition { d, _ ->
        for ((id, town) in d.stationTowns) {
            assertTrue("${d.code} station id '$id'", id.isNotBlank() && id == id.trim())
            assertTrue("${d.code} town for $id", town.isNotBlank())
        }
    }

    @Test
    fun boundsAreOrdered() = eachDefinition { d, _ ->
        for ((id, b) in d.terrainBounds + ("state" to d.stateBounds) + ("view" to d.pickerMap.view)) {
            assertTrue("${d.code} $id lon order", b.minLon < b.maxLon)
            assertTrue("${d.code} $id lat order", b.minLat < b.maxLat)
        }
    }

    @Test
    fun theOutlineHasACoastAndALandBorderInsideTheStatePicture() = eachDefinition { d, _ ->
        assertTrue("${d.code} coast", d.outline.coast.size >= 2)
        assertTrue("${d.code} land border", d.outline.landBorder.size >= 2)
        for ((lon, lat) in d.outline.coast + d.outline.landBorder) {
            assertTrue("${d.code} outline point $lon, $lat", inside(d.stateBounds, lon, lat))
        }
    }

    @Test
    fun thePickerMapArtworkStaysInsideItsWindow() = eachDefinition { d, ids ->
        val art = d.pickerMap
        assertTrue("${d.code} sea", art.seaPoints.size >= 2)
        for ((lon, lat) in art.seaPoints) assertTrue("${d.code} sea $lon, $lat", inside(art.view, lon.toDouble(), lat.toDouble()))
        assertTrue("${d.code} sea bottom", art.seaBottomLon.toDouble() in art.view.minLon..art.view.maxLon)
        for (lat in art.gridLatitudes) assertTrue("${d.code} grid $lat", lat.toDouble() in art.view.minLat..art.view.maxLat)
        for (ridge in art.ridges) {
            assertTrue("${d.code} ridge", ridge.size >= 2)
            for ((lon, lat) in ridge) assertTrue("${d.code} ridge $lon, $lat", inside(art.view, lon.toDouble(), lat.toDouble()))
        }
        assertEquals("${d.code} one name per region", ids.size, art.regionLabels.size)
        for (l in art.regionLabels + art.neighbourLabels) {
            assertNotNull(l)
            assertTrue("${d.code} label text", l.textRes != 0)
            assertTrue("${d.code} label ${l.lon}, ${l.lat}", inside(art.view, l.lon.toDouble(), l.lat.toDouble()))
        }
        assertTrue("${d.code} border line", art.borderLine.lat.toDouble() in art.view.minLat..art.view.maxLat)
    }
}

package io.github.genesjc.climatefinder.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.genesjc.climatefinder.config.StateUsaCA

/** The regions and facts of the California definition (the checks every definition must pass are in StateUsaDefinitionContractTest). */
class StateUsaCATest {
    private val regions = StateUsaCA.regions

    @Test
    fun stationTownsNameATownForEveryEntry() {
        val towns = StateUsaCA.stationTowns
        assertTrue(towns.isNotEmpty())
        assertTrue(towns.all { (id, town) -> id.isNotBlank() && town.isNotBlank() })
        assertEquals("Jackson", towns["MTZC1"])
        assertEquals("Sacramento", towns["KSMF"])
        assertEquals("Sacramento", towns["KSAC"])
    }

    @Test
    fun thereAreSixRegionsInTheirNorthToSouthOrder() {
        assertEquals(
            listOf(
                "northern_california", "bay_area_sacramento", "central_coast",
                "central_valley", "los_angeles", "san_diego"
            ),
            regions.map { it.id }
        )
    }

    @Test
    fun idsAndNamesAreUnique() {
        assertEquals(regions.size, regions.map { it.id }.toSet().size)
        assertEquals(regions.size, regions.map { it.nameRes }.toSet().size)
    }

    @Test
    fun everyRegionHasItsTextFilledIn() {
        for (r in regions) {
            assertTrue("${r.id} name", r.nameRes != 0)
            assertTrue("${r.id} title", r.titleRes != 0)
            assertTrue("${r.id} subtitle", r.subtitleRes != 0)
            assertTrue("${r.id} description", r.descriptionRes != 0)
            assertTrue("${r.id} key cities", r.keyCities.isNotEmpty())
            assertTrue("${r.id} traits", r.microclimateTraitRes.isNotEmpty() && r.microclimateTraitRes.all { it != 0 })
        }
    }

    @Test
    fun boundsAreOrderedAndInsideCalifornia() {
        for (r in regions) {
            assertTrue("${r.id} lat order", r.minLat < r.maxLat)
            assertTrue("${r.id} lon order", r.minLon < r.maxLon)
            assertTrue("${r.id} lat range", r.minLat >= 32.0 && r.maxLat <= 42.1)
            assertTrue("${r.id} lon range", r.minLon >= -124.6 && r.maxLon <= -114.0)
        }
    }

    @Test
    fun centresSitInsideTheirOwnBounds() {
        for (r in regions) {
            assertTrue("${r.id} centre lat", r.centerLat in r.minLat..r.maxLat)
            assertTrue("${r.id} centre lon", r.centerLon in r.minLon..r.maxLon)
        }
    }

    @Test
    fun elevationRangesAreOrdered() {
        for (r in regions) assertTrue("${r.id} elevation", r.minElevationMeters < r.maxElevationMeters)
    }

    @Test
    fun colourIsAHexTripletAndEveryRegionHasItsOwn() {
        val hex = Regex("#[0-9A-Fa-f]{6}")
        for (r in regions) assertTrue("${r.id} colour ${r.colorHex}", hex.matches(r.colorHex))
        assertEquals(regions.size, regions.map { it.colorHex.uppercase() }.toSet().size)
    }

    @Test
    fun theEarthToneColoursAreAsDesigned() {
        val expected = mapOf(
            "northern_california" to "#7C6A8C",
            "bay_area_sacramento" to "#5F8F7C",
            "central_valley" to "#B8733A",
            "central_coast" to "#C9857E",
            "los_angeles" to "#6C86A8",
            "san_diego" to "#5E7F49"
        )
        for (r in regions) assertEquals(r.id, expected.getValue(r.id), r.colorHex.uppercase())
    }
}

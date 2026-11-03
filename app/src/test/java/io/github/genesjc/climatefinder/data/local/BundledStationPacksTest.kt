package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.config.StateUsaCA
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decodes the six station packs that ship in assets (a.txt to f.txt) and checks each holds the
 * expected number of stations, so a stale or damaged pack is caught the moment it is committed.
 * Update the counts here when the station data changes.
 */
class BundledStationPacksTest {

    // Pack file, region, expected number of station rows
    private val packs = listOf(
        Triple("a.txt", "Northern California", 60),
        Triple("b.txt", "Bay Area / Sacramento", 57),
        Triple("c.txt", "Central Coast", 55),
        Triple("d.txt", "Central Valley", 59),
        Triple("e.txt", "Los Angeles", 58),
        Triple("f.txt", "San Diego", 50)
    )

    // Unit tests run from the app module directory
    private fun packText(fileName: String) = File("src/main/assets/$fileName").readText()

    @Test
    fun everyPackDecodesToItsStations() {
        for ((fileName, region, expectedRows) in packs) {
            val csv = StationPack.decode(packText(fileName))
            assertNotNull("$fileName ($region) did not decode", csv)
            val lines = csv!!.lines().filter { it.isNotBlank() }
            assertEquals("$fileName ($region) station count", expectedRows, lines.size - 1)
        }
    }

    @Test
    fun everyPackHasTheColumnsTheParserReads() {
        for ((fileName, region, _) in packs) {
            val header = StationPack.decode(packText(fileName))!!.lines().first().lowercase().split(",")
            for (column in listOf("id", "name", "latitude", "longitude", "elevation_meters", "terrain_type", "tier")) {
                assertTrue("$fileName ($region) is missing the '$column' column", column in header)
            }
        }
    }

    @Test
    fun everyLandmarkStationIsInItsRegionsPack() {
        val regionPack = mapOf(
            "northern_california" to "a.txt", "bay_area_sacramento" to "b.txt", "central_coast" to "c.txt",
            "central_valley" to "d.txt", "los_angeles" to "e.txt", "san_diego" to "f.txt"
        )
        for ((regionId, ids) in StateUsaCA.landmarkStations) {
            val pack = regionPack.getValue(regionId)
            val stationIds = StationPack.decode(packText(pack))!!.lines().drop(1).filter { it.isNotBlank() }.map { it.substringBefore(",") }.toSet()
            for (id in ids) assertTrue("$id is not in $pack ($regionId)", id in stationIds)
        }
    }

    @Test
    fun everyRegionHasFeaturedStationsForThePrefetch() {
        for ((fileName, region, _) in packs) {
            val lines = StationPack.decode(packText(fileName))!!.lines().filter { it.isNotBlank() }
            // tier is the last column, so read it from the end (an earlier field may contain commas)
            assertEquals("$fileName ($region) last column", "tier", lines.first().lowercase().split(",").last().trim())
            val featured = lines.drop(1).count { it.substringAfterLast(",").trim() == "featured" }
            assertTrue("$fileName ($region) has no featured stations", featured > 0)
        }
    }
}

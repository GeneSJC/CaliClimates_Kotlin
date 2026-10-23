package io.github.genesjc.microclimatefinder.data.local

import io.github.genesjc.microclimatefinder.domain.model.StateUsaCA
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the terrain pictures that ship in assets (terrain/<regionId>.webp): every region has one,
 * it is a WebP of a sensible size, and its bounds cover that region's stations with room around them.
 * If the station data changes, regenerate the pictures so the stations stay inside.
 */
class TerrainMapsTest {

    // Station pack file and the region it holds
    private val packs = listOf(
        "a.txt" to "northern_california",
        "b.txt" to "bay_area_sacramento",
        "c.txt" to "central_coast",
        "d.txt" to "central_valley",
        "e.txt" to "los_angeles",
        "f.txt" to "san_diego"
    )

    // Unit tests run from the app module directory
    private fun stationPoints(packFile: String): List<Pair<Double, Double>> {
        val lines = StationPack.decode(File("src/main/assets/$packFile").readText())!!.lines().filter { it.isNotBlank() }
        val header = lines.first().lowercase().split(",")
        val latIndex = header.indexOf("latitude")
        val lonIndex = header.indexOf("longitude")
        return lines.drop(1).mapNotNull { line ->
            val fields = line.split(",")
            val lat = fields.getOrNull(latIndex)?.toDoubleOrNull()
            val lon = fields.getOrNull(lonIndex)?.toDoubleOrNull()
            if (lat != null && lon != null) lat to lon else null
        }
    }

    @Test
    fun everyRegionHasATerrainPicture() {
        for ((_, regionId) in packs) {
            assertNotNull("No bounds for $regionId", StateUsaCA.terrainBounds[regionId])
            val file = File("src/main/assets/${TerrainMaps.assetPath(regionId)}")
            assertTrue("${file.path} is missing", file.exists())
            // A WebP file starts with "RIFF", then a size, then "WEBP"
            val head = file.inputStream().use { it.readNBytes(12) }
            assertEquals("$regionId is not a WebP", "RIFF", String(head, 0, 4))
            assertEquals("$regionId is not a WebP", "WEBP", String(head, 8, 4))
            assertTrue("$regionId picture is larger than expected (${file.length()} bytes)", file.length() < 600_000)
        }
    }

    @Test
    fun everyRegionHasASkyline() {
        for ((_, regionId) in packs) {
            val file = File("src/main/assets/${TerrainMaps.skylineAssetPath(regionId)}")
            assertTrue("${file.path} is missing", file.exists())
            // Three layers (far, middle, near) of 160 numbers from 0 to 255
            val lines = file.readLines().filter { it.isNotBlank() }
            assertEquals("$regionId skyline layers", 3, lines.size)
            for (line in lines) {
                val values = line.split(",").map { it.trim().toInt() }
                assertEquals("$regionId skyline length", 160, values.size)
                assertTrue("$regionId skyline has a value outside 0 to 255", values.all { it in 0..255 })
            }
        }
    }

    @Test
    fun everyPictureCoversItsStationsWithRoomAround() {
        for ((packFile, regionId) in packs) {
            val bounds = StateUsaCA.terrainBounds.getValue(regionId)
            val points = stationPoints(packFile)
            assertTrue("$regionId has no stations", points.isNotEmpty())
            val lats = points.map { it.first }
            val lons = points.map { it.second }
            // The map frames the stations with an 8% margin; the picture has to reach past that
            val latMargin = (lats.max() - lats.min()) * 0.08
            val lonMargin = (lons.max() - lons.min()) * 0.08
            assertTrue("$regionId: stations pass the south edge", bounds.minLat <= lats.min() - latMargin)
            assertTrue("$regionId: stations pass the north edge", bounds.maxLat >= lats.max() + latMargin)
            assertTrue("$regionId: stations pass the west edge", bounds.minLon <= lons.min() - lonMargin)
            assertTrue("$regionId: stations pass the east edge", bounds.maxLon >= lons.max() + lonMargin)
        }
    }
}

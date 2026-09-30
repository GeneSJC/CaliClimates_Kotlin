package io.github.genesjc.microclimatefinder.data.local

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** A box of longitude and latitude. */
data class GeoBounds(val minLon: Double, val maxLon: Double, val minLat: Double, val maxLat: Double)

/** A shaded-relief picture of a region and the ground it covers. Its pixels are linear in lon and lat. */
class TerrainImage(val image: ImageBitmap, val bounds: GeoBounds)

/**
 * The soft terrain picture drawn under the stations on the Topography map: one image per region in
 * the app's assets (`terrain/<regionId>.webp`), each covering the region's stations and the ground
 * around them. They are built outside the app from public elevation data (USGS 3DEP, NASA SRTM and
 * other public sources, via AWS Terrain Tiles) and shaded for a zoomed-out feel of the landscape.
 */
object TerrainMaps {
    private const val TAG = "TerrainMaps"

    /** What each picture covers. The images are generated to these bounds; a unit test checks the stations fit. */
    val BOUNDS: Map<String, GeoBounds> = mapOf(
        "northern_california" to GeoBounds(-125.4572, -118.9025, 37.3110, 43.8160),
        "bay_area_sacramento" to GeoBounds(-123.8959, -119.0628, 36.3508, 39.8245),
        "central_coast" to GeoBounds(-123.3826, -117.9792, 33.7269, 37.6208),
        "central_valley" to GeoBounds(-123.7687, -115.9850, 34.6013, 39.3836),
        "los_angeles" to GeoBounds(-119.3960, -116.5571, 32.3232, 35.9385),
        "san_diego" to GeoBounds(-117.7634, -115.8557, 31.7898, 34.2508)
    )

    fun assetPath(regionId: String) = "terrain/$regionId.webp"

    /** Loads the region's picture, or null when it has none (the map then falls back to a plain base). */
    fun load(context: Context, regionId: String): TerrainImage? {
        val bounds = BOUNDS[regionId] ?: return null
        return try {
            context.assets.open(assetPath(regionId)).use { BitmapFactory.decodeStream(it) }
                ?.let { TerrainImage(it.asImageBitmap(), bounds) }
        } catch (e: Exception) {
            Log.w(TAG, "No terrain picture for $regionId", e)
            null
        }
    }
}

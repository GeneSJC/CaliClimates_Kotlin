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
        // TERRAIN_BOUNDS
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

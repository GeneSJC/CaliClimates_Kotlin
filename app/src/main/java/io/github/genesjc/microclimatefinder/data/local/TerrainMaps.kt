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

/** The California picker's terrain: land inside California, and the neighbouring land (faded). Same bounds. */
class StateTerrain(val state: ImageBitmap, val surround: ImageBitmap)

/**
 * The soft terrain picture drawn under the stations on the Topography map: one image per region in
 * the app's assets (`terrain/<regionId>.webp`), each covering the region's stations and the ground
 * around them. They are built outside the app from public elevation data (USGS 3DEP, NASA SRTM and
 * other public sources, via AWS Terrain Tiles) and shaded for a zoomed-out feel of the landscape.
 * This object holds only the fixed facts about the pictures; [TerrainSource] reads them.
 */
object TerrainMaps {
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

    fun skylineAssetPath(regionId: String) = "terrain/skyline_$regionId.txt"

    /** The whole state for the California picker, and its neighbours drawn quieter. Their pixels are linear in lon and lat. */
    val STATE_BOUNDS = GeoBounds(-124.5, -114.1, 31.9, 43.0)
}

/** Where the terrain pictures are read from (the app's assets; a fake in tests). Every call may be slow: call off the main thread. */
interface TerrainSource {
    /** The region's picture, or null when it has none (the map then falls back to a plain base). */
    fun load(regionId: String): TerrainImage?

    /**
     * Real skylines of the region, for the Elevation view's distant ranges: three lines of 160 numbers (0 to 1), west to
     * east, for the far, middle and near ranges (the highest ground, the 85th and the 60th percentile of each column).
     * Null when the region has none.
     */
    fun loadSkyline(regionId: String): List<FloatArray>?

    /** The statewide pictures for the California picker, or null when missing. */
    fun loadState(): StateTerrain?

    /** The relief texture behind the light backdrop, or null when it is missing. */
    fun loadBackdropRelief(): ImageBitmap?
}

/** Reads the terrain pictures from the app's assets. */
class AssetTerrainSource(context: Context) : TerrainSource {
    private val assets = context.applicationContext.assets

    // The statewide pictures are about 18 MB once decoded, so they are kept after the first decode and handed out again
    // on every later open of the picker. A soft reference lets the system take the memory back if it is short.
    private var stateCache: java.lang.ref.SoftReference<StateTerrain>? = null

    override fun load(regionId: String): TerrainImage? {
        val bounds = TerrainMaps.BOUNDS[regionId] ?: return null
        return try {
            assets.open(TerrainMaps.assetPath(regionId)).use { BitmapFactory.decodeStream(it) }
                ?.let { TerrainImage(it.asImageBitmap(), bounds) }
        } catch (e: Exception) {
            Log.w(TAG, "No terrain picture for $regionId", e)
            null
        }
    }

    override fun loadSkyline(regionId: String): List<FloatArray>? =
        try {
            assets.open(TerrainMaps.skylineAssetPath(regionId)).bufferedReader().use { reader ->
                reader.readLines().filter { it.isNotBlank() }.map { line ->
                    line.split(",").map { it.trim().toFloat() / 255f }.toFloatArray()
                }
            }.takeIf { it.size == 3 && it.all { layer -> layer.size >= 2 } }
        } catch (e: Exception) {
            Log.w(TAG, "No skyline for $regionId", e)
            null
        }

    @Synchronized
    override fun loadState(): StateTerrain? {
        stateCache?.get()?.let { return it }
        val loaded = try {
            fun decode(name: String) = assets.open("terrain/$name").use { BitmapFactory.decodeStream(it) }
            val state = decode("california.webp")
            val surround = decode("california_surround.webp")
            if (state != null && surround != null) StateTerrain(state.asImageBitmap(), surround.asImageBitmap()) else null
        } catch (e: Exception) {
            Log.w(TAG, "No statewide terrain picture", e)
            null
        }
        if (loaded != null) stateCache = java.lang.ref.SoftReference(loaded)
        return loaded
    }

    override fun loadBackdropRelief(): ImageBitmap? =
        try {
            assets.open("terrain/bg_relief.webp").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        } catch (e: Exception) {
            Log.w(TAG, "No backdrop relief picture", e)
            null
        }

    private companion object {
        const val TAG = "TerrainMaps"
    }
}

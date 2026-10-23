package io.github.genesjc.microclimatefinder.data.local

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.genesjc.microclimatefinder.domain.model.GeoBounds
import io.github.genesjc.microclimatefinder.domain.model.StateUsaDefinition

/** A shaded-relief picture of a region and the ground it covers. Its pixels are linear in lon and lat. */
class TerrainImage(val image: ImageBitmap, val bounds: GeoBounds)

/** The picker's terrain: land inside the state, and the neighbouring land (faded). Same bounds. */
class StateUsaTerrain(val state: ImageBitmap, val surround: ImageBitmap)

/**
 * The soft terrain picture drawn under the stations on the Topography map: one image per region in
 * the app's assets (`terrain/<regionId>.webp`), each covering the region's stations and the ground
 * around them. They are built outside the app from public elevation data (USGS 3DEP, NASA SRTM and
 * other public sources, via AWS Terrain Tiles) and shaded for a zoomed-out feel of the landscape.
 * This object holds only the names of the pictures; the ground each covers is the state definition's; [TerrainSource] reads them.
 */
object TerrainMaps {
    fun assetPath(regionId: String) = "terrain/$regionId.webp"

    fun skylineAssetPath(regionId: String) = "terrain/skyline_$regionId.txt"
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

    /** The statewide pictures for the picker, or null when missing. */
    fun loadState(): StateUsaTerrain?

    /** The relief texture behind the light backdrop, or null when it is missing. */
    fun loadBackdropRelief(): ImageBitmap?
}

/** Reads the terrain pictures from the app's assets. */
class AssetTerrainSource(context: Context, private val stateUsa: StateUsaDefinition) : TerrainSource {
    private val assets = context.applicationContext.assets

    // The statewide pictures are about 18 MB once decoded, so they are kept after the first decode and handed out again
    // on every later open of the picker. A soft reference lets the system take the memory back if it is short.
    private var stateCache: java.lang.ref.SoftReference<StateUsaTerrain>? = null

    override fun load(regionId: String): TerrainImage? {
        val bounds = stateUsa.terrainBounds[regionId] ?: return null
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
    override fun loadState(): StateUsaTerrain? {
        stateCache?.get()?.let { return it }
        val loaded = try {
            fun decode(name: String) = assets.open("terrain/$name").use { BitmapFactory.decodeStream(it) }
            val state = decode("california.webp")
            val surround = decode("california_surround.webp")
            if (state != null && surround != null) StateUsaTerrain(state.asImageBitmap(), surround.asImageBitmap()) else null
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

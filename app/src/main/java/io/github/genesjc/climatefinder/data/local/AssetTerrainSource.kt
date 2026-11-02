package io.github.genesjc.climatefinder.data.local

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.genesjc.climatefinder.domain.model.StateUsaDefinition

/** Reads the terrain pictures from the app's assets. */
class AssetTerrainSource(context: Context, private val stateUsa: StateUsaDefinition) : TerrainSource {
    private val assets = context.applicationContext.assets

    // The statewide pictures are about 18 MB once decoded, so they are kept after the first decode and handed out again
    // on every later open of the picker. A soft reference lets the system take the memory back if it is short.
    private var stateCache: java.lang.ref.SoftReference<StateUsaTerrain>? = null

    override fun load(regionId: String): TerrainImage? {
        val bounds = stateUsa.terrainBounds[regionId] ?: return null
        return try {
            assets.open(TerrainAssetPaths.assetPath(regionId)).use { BitmapFactory.decodeStream(it) }
                ?.let { TerrainImage(it.asImageBitmap(), bounds) }
        } catch (e: Exception) {
            Log.w(TAG, "No terrain picture for $regionId", e)
            null
        }
    }

    override fun loadSkyline(regionId: String): List<FloatArray>? =
        try {
            assets.open(TerrainAssetPaths.skylineAssetPath(regionId)).bufferedReader().use { reader ->
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

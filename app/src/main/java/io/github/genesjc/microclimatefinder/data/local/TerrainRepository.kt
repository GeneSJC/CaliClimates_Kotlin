package io.github.genesjc.microclimatefinder.data.local

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The terrain pictures, read off the main thread. It owns the IO dispatcher, so a ViewModel just calls these suspending
 * functions; [ioDispatcher] is a default argument that tests replace.
 */
class TerrainRepository(
    private val source: TerrainSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /** The region's picture, or null when it has none. */
    suspend fun terrain(regionId: String): TerrainImage? = withContext(ioDispatcher) { source.load(regionId) }

    /** The region's real skylines for the Elevation view's distant ranges, or null when it has none. */
    suspend fun skyline(regionId: String): List<FloatArray>? = withContext(ioDispatcher) { source.loadSkyline(regionId) }

    /** The statewide pictures for the picker, or null when missing. */
    suspend fun stateTerrain(): StateUsaTerrain? = withContext(ioDispatcher) { source.loadState() }

    /** The relief texture behind the light backdrop, or null when it is missing. */
    suspend fun backdropRelief(): ImageBitmap? = withContext(ioDispatcher) { source.loadBackdropRelief() }
}

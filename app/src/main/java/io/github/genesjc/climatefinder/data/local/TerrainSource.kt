package io.github.genesjc.climatefinder.data.local

import android.graphics.Bitmap

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
    fun loadBackdropRelief(): Bitmap?
}

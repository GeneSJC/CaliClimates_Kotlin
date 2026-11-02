package io.github.genesjc.climatefinder.data.local

/**
 * The soft terrain picture drawn under the stations on the Topography map: one image per region in
 * the app's assets (`terrain/<regionId>.webp`), each covering the region's stations and the ground
 * around them. They are built outside the app from public elevation data (USGS 3DEP, NASA SRTM and
 * other public sources, via AWS Terrain Tiles) and shaded for a zoomed-out feel of the landscape.
 * This object holds only the names of the pictures; the ground each covers is the state definition's; [TerrainSource] reads them.
 */
object TerrainAssetPaths {
    fun assetPath(regionId: String) = "terrain/$regionId.webp"

    fun skylineAssetPath(regionId: String) = "terrain/skyline_$regionId.txt"
}


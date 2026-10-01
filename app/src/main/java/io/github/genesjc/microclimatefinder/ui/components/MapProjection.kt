package io.github.genesjc.microclimatefinder.ui.components

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/**
 * Where longitude and latitude land on a map canvas, before zoom and pan. One scale in both
 * directions, so a region keeps its true shape: the frame (the bounds given) is fitted inside the
 * canvas and centered, and any extra canvas shows the ground beyond the frame.
 */
class MapProjection(
    private val minLon: Double,
    maxLon: Double,
    minLat: Double,
    private val maxLat: Double,
    canvasW: Float,
    canvasH: Float,
    padding: Float
) {
    private val cosMid = cos((minLat + maxLat) / 2.0 * PI / 180.0)
    private val frameW = (maxLon - minLon) * cosMid
    private val frameH = maxLat - minLat

    /** Canvas pixels per degree of latitude. */
    private val scale: Double = min((canvasW - padding * 2) / frameW, (canvasH - padding * 2) / frameH)
    private val offsetX = padding + ((canvasW - padding * 2) - frameW * scale) / 2.0
    private val offsetY = padding + ((canvasH - padding * 2) - frameH * scale) / 2.0

    /** Canvas pixels per kilometre at zoom 1 (a degree of latitude is about 111.2 km). */
    val pixelsPerKm: Double = scale / 111.19

    fun baseX(lon: Double): Float = (offsetX + (lon - minLon) * cosMid * scale).toFloat()
    fun baseY(lat: Double): Float = (offsetY + (maxLat - lat) * scale).toFloat()
}

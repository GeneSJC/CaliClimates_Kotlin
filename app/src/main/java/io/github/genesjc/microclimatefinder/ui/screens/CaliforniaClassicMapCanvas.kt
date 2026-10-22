package io.github.genesjc.microclimatefinder.ui.screens

import io.github.genesjc.microclimatefinder.data.local.StateTerrain
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions

/**
 * Classic CA: Realistic Geographic Silhouette of California
 * With accurate state bounds (42°N Oregon border, Lake Tahoe corner, diagonal Nevada border,
 * Point Conception coastal hook, down to 32.5°N Mexico border) and 6 interlocking macro sections.
 * Rendered in strict Light Mode.
 */

/**
 * Classic CA: Realistic Geographic Silhouette of California
 * With continuous interlocking regional topology:
 * - North Coast, Cascade & Sierra Range
 * - Bay Area & Sacramento River Transect
 * - Central Valley agricultural basin
 * - Central Coast (Big Sur to Santa Barbara)
 * - Los Angeles & Basin
 * - San Diego & South Coast
 * All 6 regional polygons share identical boundary vertices, guaranteeing ZERO interior gaps.
 * Rendered in strict Light Mode with subtle atmospheric grid lines.
 */
@Composable
internal fun CaliforniaClassicMapCanvas(
    selectedRegionId: String,
    terrain: StateTerrain?,
    onSelectRegion: (String) -> Unit,
    sizeModifier: Modifier = Modifier.height(430.dp),
    // Stretches the map vertically (1 = true shape); the picker uses a little, to be taller
    verticalStretch: Float = 1f
) {
    val tintAlpha = if (terrain != null) 0.58f else 0.72f
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(sizeModifier)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFBEB), Color(0xFFF3E1B8), Color(0xFFE6CD98)),
                    radius = 900f
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.5.dp, Color(0xFFC9A66B), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val lonMin = -124.45f
                    val lonMax = -114.15f
                    val latMin = 31.9f
                    val latMax = 43.0f
                    // True shape: one scale for both axes (a degree of longitude is about 0.8 of a degree of latitude), centred
                    val k = minOf(w * 0.985f / ((lonMax - lonMin) * 0.8f), h * 0.985f / ((latMax - latMin) * verticalStretch))
                    val availW = (lonMax - lonMin) * 0.8f * k
                    val availH = (latMax - latMin) * k * verticalStretch
                    val padX = (w - availW) / 2f
                    val padY = (h - availH) / 2f

                    fun px(lon: Float): Float = padX + ((lon - lonMin) / (lonMax - lonMin)) * availW
                    fun py(lat: Float): Float = padY + (1.0f - (lat - latMin) / (latMax - latMin)) * availH

                    fun inPoly(pt: Offset, poly: List<Offset>): Boolean {
                        var inside = false
                        var j = poly.size - 1
                        for (i in poly.indices) {
                            val xi = poly[i].x
                            val yi = poly[i].y
                            val xj = poly[j].x
                            val yj = poly[j].y
                            val intersect = ((yi > pt.y) != (yj > pt.y)) &&
                                    (pt.x < (xj - xi) * (pt.y - yi) / (yj - yi) + xi)
                            if (intersect) inside = !inside
                            j = i
                        }
                        return inside
                    }

                    // Shared boundary vertices (lon, lat)
                    val pTahoe = Offset(px(-120.00f), py(39.00f))
                    val pNorthValleyMid = Offset(px(-120.80f), py(39.15f))
                    val tNorthBayValley = Offset(px(-121.60f), py(39.20f))
                    val pNorthBayMid = Offset(px(-122.80f), py(38.95f))
                    val pCoastMendoSonoma = Offset(px(-123.65f), py(38.80f))

                    val pBayValley1 = Offset(px(-121.20f), py(38.50f))
                    val pBayValley2 = Offset(px(-121.15f), py(37.95f))
                    val pBayValley3 = Offset(px(-121.30f), py(37.50f))
                    val tBayValleyCoast = Offset(px(-121.50f), py(37.00f))
                    val pBayCoastMid = Offset(px(-121.80f), py(36.95f))
                    val pCoastMontereyBay = Offset(px(-121.85f), py(36.80f))

                    val pCoastValley1 = Offset(px(-120.70f), py(36.20f))
                    val pCoastValley2 = Offset(px(-120.10f), py(35.50f))
                    val pCoastValley3 = Offset(px(-119.50f), py(35.00f))
                    val tCoastValleyLa = Offset(px(-119.00f), py(34.80f))
                    val pCoastLaMid = Offset(px(-119.30f), py(34.45f))
                    val pCoastVentura = Offset(px(-119.10f), py(34.10f))

                    val pValleyLa1 = Offset(px(-118.45f), py(35.05f))
                    val pValleyLa2 = Offset(px(-117.80f), py(35.35f))
                    val pValleyLa3 = Offset(px(-117.20f), py(35.80f))
                    val pNevadaValleyLa = Offset(px(-115.80f), py(36.00f))

                    val pCoastSanOnofre = Offset(px(-117.60f), py(33.38f))
                    val pLaSd1 = Offset(px(-117.10f), py(33.45f))
                    val pLaSd2 = Offset(px(-116.30f), py(33.35f))
                    val pLaSd3 = Offset(px(-115.50f), py(33.20f))
                    val pColoradoRiverMid = Offset(px(-114.60f), py(33.30f))

                    val northPoly = listOf(
                        pCoastMendoSonoma,
                        Offset(px(-124.41f), py(40.44f)),
                        Offset(px(-124.21f), py(42.00f)),
                        Offset(px(-120.00f), py(42.00f)),
                        pTahoe,
                        pNorthValleyMid,
                        tNorthBayValley,
                        pNorthBayMid
                    )

                    val bayPoly = listOf(
                        pCoastMendoSonoma,
                        pNorthBayMid,
                        tNorthBayValley,
                        pBayValley1,
                        pBayValley2,
                        pBayValley3,
                        tBayValleyCoast,
                        pBayCoastMid,
                        pCoastMontereyBay,
                        Offset(px(-122.20f), py(37.00f)),
                        Offset(px(-122.51f), py(37.77f)),
                        Offset(px(-123.00f), py(38.00f))
                    )

                    val valleyPoly = listOf(
                        pTahoe,
                        pNorthValleyMid,
                        tNorthBayValley,
                        pBayValley1,
                        pBayValley2,
                        pBayValley3,
                        tBayValleyCoast,
                        pCoastValley1,
                        pCoastValley2,
                        pCoastValley3,
                        tCoastValleyLa,
                        pValleyLa1,
                        pValleyLa2,
                        pValleyLa3,
                        pNevadaValleyLa,
                        Offset(px(-117.20f), py(37.00f)),
                        Offset(px(-118.60f), py(38.00f))
                    )

                    val coastPoly = listOf(
                        pCoastMontereyBay,
                        pBayCoastMid,
                        tBayValleyCoast,
                        pCoastValley1,
                        pCoastValley2,
                        pCoastValley3,
                        tCoastValleyLa,
                        pCoastLaMid,
                        pCoastVentura,
                        Offset(px(-119.70f), py(34.40f)),
                        Offset(px(-120.47f), py(34.45f)),
                        Offset(px(-120.85f), py(35.30f)),
                        Offset(px(-121.90f), py(36.30f))
                    )

                    val laPoly = listOf(
                        pCoastVentura,
                        pCoastLaMid,
                        tCoastValleyLa,
                        pValleyLa1,
                        pValleyLa2,
                        pValleyLa3,
                        pNevadaValleyLa,
                        Offset(px(-114.63f), py(35.00f)),
                        pColoradoRiverMid,
                        pLaSd3,
                        pLaSd2,
                        pLaSd1,
                        pCoastSanOnofre,
                        Offset(px(-118.40f), py(33.74f)),
                        Offset(px(-118.85f), py(34.02f))
                    )

                    val sdPoly = listOf(
                        pCoastSanOnofre,
                        pLaSd1,
                        pLaSd2,
                        pLaSd3,
                        pColoradoRiverMid,
                        Offset(px(-114.72f), py(32.72f)),
                        Offset(px(-117.12f), py(32.53f)),
                        Offset(px(-117.25f), py(32.72f))
                    )

                    when {
                        inPoly(offset, northPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.NORTHERN_CALIFORNIA.id)
                        inPoly(offset, bayPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
                        inPoly(offset, valleyPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.CENTRAL_VALLEY.id)
                        inPoly(offset, coastPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.CENTRAL_COAST.id)
                        inPoly(offset, laPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.LOS_ANGELES.id)
                        inPoly(offset, sdPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.SAN_DIEGO.id)
                        else -> {
                            val tapLon = lonMin + ((offset.x - padX) / availW) * (lonMax - lonMin)
                            val tapLat = latMin + (1f - (offset.y - padY) / availH) * (latMax - latMin)
                            val closest = CaliforniaRegionalDefinitions.ALL_REGIONS.minByOrNull { r ->
                                val dLat = tapLat - r.centerLat
                                val dLon = tapLon - r.centerLon
                                dLat * dLat + dLon * dLon
                            }
                            closest?.let { onSelectRegion(it.id) }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val lonMin = -124.45f
            val lonMax = -114.15f
            val latMin = 31.9f
            val latMax = 43.0f
            // True shape: one scale for both axes (a degree of longitude is about 0.8 of a degree of latitude), centred
            val k = minOf(w * 0.985f / ((lonMax - lonMin) * 0.8f), h * 0.985f / ((latMax - latMin) * verticalStretch))
            val availW = (lonMax - lonMin) * 0.8f * k
            val availH = (latMax - latMin) * k * verticalStretch
            val padX = (w - availW) / 2f
            val padY = (h - availH) / 2f

            fun px(lon: Float): Float = padX + ((lon - lonMin) / (lonMax - lonMin)) * availW
            fun py(lat: Float): Float = padY + (1.0f - (lat - latMin) / (latMax - latMin)) * availH

            // 1. Warm-toned dashed graticule lines (34°N, 36°N, 38°N, 40°N) — antique cartography feel
            val graticuleEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            val gridLats = floatArrayOf(34f, 36f, 38f, 40f)
            for (glat in gridLats) {
                val y = py(glat)
                drawLine(
                    color = Color(0xFFB89968),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f,
                    pathEffect = graticuleEffect,
                    alpha = 0.55f
                )
            }

            // Decorative compass rose watermark (top-right), for character and a sense of place
            drawCompassRose(center = Offset(w - 34f, 34f), radius = 22f, color = Color(0xFF8B6F3E))

            // Shared boundary vertices (Continuous Interlocking Topology)
            val pTahoe = Offset(px(-120.00f), py(39.00f))
            val pNorthValleyMid = Offset(px(-120.80f), py(39.15f))
            val tNorthBayValley = Offset(px(-121.60f), py(39.20f))
            val pNorthBayMid = Offset(px(-122.80f), py(38.95f))
            val pCoastMendoSonoma = Offset(px(-123.65f), py(38.80f))

            val pBayValley1 = Offset(px(-121.20f), py(38.50f))
            val pBayValley2 = Offset(px(-121.15f), py(37.95f))
            val pBayValley3 = Offset(px(-121.30f), py(37.50f))
            val tBayValleyCoast = Offset(px(-121.50f), py(37.00f))
            val pBayCoastMid = Offset(px(-121.80f), py(36.95f))
            val pCoastMontereyBay = Offset(px(-121.85f), py(36.80f))

            val pCoastValley1 = Offset(px(-120.70f), py(36.20f))
            val pCoastValley2 = Offset(px(-120.10f), py(35.50f))
            val pCoastValley3 = Offset(px(-119.50f), py(35.00f))
            val tCoastValleyLa = Offset(px(-119.00f), py(34.80f))
            val pCoastLaMid = Offset(px(-119.30f), py(34.45f))
            val pCoastVentura = Offset(px(-119.10f), py(34.10f))

            val pValleyLa1 = Offset(px(-118.45f), py(35.05f))
            val pValleyLa2 = Offset(px(-117.80f), py(35.35f))
            val pValleyLa3 = Offset(px(-117.20f), py(35.80f))
            val pNevadaValleyLa = Offset(px(-115.80f), py(36.00f))

            val pCoastSanOnofre = Offset(px(-117.60f), py(33.38f))
            val pLaSd1 = Offset(px(-117.10f), py(33.45f))
            val pLaSd2 = Offset(px(-116.30f), py(33.35f))
            val pLaSd3 = Offset(px(-115.50f), py(33.20f))
            val pColoradoRiverMid = Offset(px(-114.60f), py(33.30f))

            fun makePath(pts: List<Offset>): Path {
                val path = Path()
                if (pts.isNotEmpty()) {
                    path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        path.lineTo(pts[i].x, pts[i].y)
                    }
                    path.close()
                }
                return path
            }

            // Pacific Ocean coastal wash
            val oceanPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(px(-124.45f), py(43.00f))
                lineTo(px(-124.21f), py(42.00f))
                lineTo(px(-124.41f), py(40.44f))
                lineTo(pCoastMendoSonoma.x, pCoastMendoSonoma.y)
                lineTo(px(-123.00f), py(38.00f))
                lineTo(px(-122.51f), py(37.77f))
                lineTo(px(-122.20f), py(37.00f))
                lineTo(pCoastMontereyBay.x, pCoastMontereyBay.y)
                lineTo(px(-121.90f), py(36.30f))
                lineTo(px(-120.85f), py(35.30f))
                lineTo(px(-120.47f), py(34.45f))
                lineTo(px(-119.70f), py(34.40f))
                lineTo(pCoastVentura.x, pCoastVentura.y)
                lineTo(px(-118.85f), py(34.02f))
                lineTo(px(-118.40f), py(33.74f))
                lineTo(pCoastSanOnofre.x, pCoastSanOnofre.y)
                lineTo(px(-117.25f), py(32.72f))
                lineTo(px(-117.12f), py(32.53f))
                lineTo(px(-117.02f), py(32.20f))
                lineTo(px(-116.70f), py(31.90f))
                lineTo(px(-116.65f), h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                oceanPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFCDEEF2), Color(0xFFA8DEE6)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            // 2. The 6 seamlessly interlocking regions (ZERO interior gaps)
            val regionPolys = listOf(
                Pair(
                    CaliforniaRegionalDefinitions.NORTHERN_CALIFORNIA,
                    makePath(listOf(
                        pCoastMendoSonoma,
                        Offset(px(-124.41f), py(40.44f)),
                        Offset(px(-124.21f), py(42.00f)),
                        Offset(px(-120.00f), py(42.00f)),
                        pTahoe,
                        pNorthValleyMid,
                        tNorthBayValley,
                        pNorthBayMid
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO,
                    makePath(listOf(
                        pCoastMendoSonoma,
                        pNorthBayMid,
                        tNorthBayValley,
                        pBayValley1,
                        pBayValley2,
                        pBayValley3,
                        tBayValleyCoast,
                        pBayCoastMid,
                        pCoastMontereyBay,
                        Offset(px(-122.20f), py(37.00f)),
                        Offset(px(-122.51f), py(37.77f)),
                        Offset(px(-123.00f), py(38.00f))
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.CENTRAL_VALLEY,
                    makePath(listOf(
                        pTahoe,
                        pNorthValleyMid,
                        tNorthBayValley,
                        pBayValley1,
                        pBayValley2,
                        pBayValley3,
                        tBayValleyCoast,
                        pCoastValley1,
                        pCoastValley2,
                        pCoastValley3,
                        tCoastValleyLa,
                        pValleyLa1,
                        pValleyLa2,
                        pValleyLa3,
                        pNevadaValleyLa,
                        Offset(px(-117.20f), py(37.00f)),
                        Offset(px(-118.60f), py(38.00f))
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.CENTRAL_COAST,
                    makePath(listOf(
                        pCoastMontereyBay,
                        pBayCoastMid,
                        tBayValleyCoast,
                        pCoastValley1,
                        pCoastValley2,
                        pCoastValley3,
                        tCoastValleyLa,
                        pCoastLaMid,
                        pCoastVentura,
                        Offset(px(-119.70f), py(34.40f)),
                        Offset(px(-120.47f), py(34.45f)),
                        Offset(px(-120.85f), py(35.30f)),
                        Offset(px(-121.90f), py(36.30f))
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.LOS_ANGELES,
                    makePath(listOf(
                        pCoastVentura,
                        pCoastLaMid,
                        tCoastValleyLa,
                        pValleyLa1,
                        pValleyLa2,
                        pValleyLa3,
                        pNevadaValleyLa,
                        Offset(px(-114.63f), py(35.00f)),
                        pColoradoRiverMid,
                        pLaSd3,
                        pLaSd2,
                        pLaSd1,
                        pCoastSanOnofre,
                        Offset(px(-118.40f), py(33.74f)),
                        Offset(px(-118.85f), py(34.02f))
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.SAN_DIEGO,
                    makePath(listOf(
                        pCoastSanOnofre,
                        pLaSd1,
                        pLaSd2,
                        pLaSd3,
                        pColoradoRiverMid,
                        Offset(px(-114.72f), py(32.72f)),
                        Offset(px(-117.12f), py(32.53f)),
                        Offset(px(-117.25f), py(32.72f))
                    ))
                )
            )

            // Soft glow halo behind the selected region, for a "pop on tap" feel
            for ((region, path) in regionPolys) {
                if (selectedRegionId == region.id) {
                    val color = Color(android.graphics.Color.parseColor(region.colorHex))
                    drawPath(path = path, color = color.copy(alpha = 0.30f), style = Stroke(width = 14f))
                }
            }

            // The land itself: shaded relief of the whole state under translucent region tints
            terrain?.let { t ->
                val dstOffset = androidx.compose.ui.unit.IntOffset(px(-124.5f).toInt(), py(43.0f).toInt())
                val dstSize = androidx.compose.ui.unit.IntSize(
                    (availW * 10.4f / (lonMax - lonMin)).toInt(),
                    (availH * 11.1f / (latMax - latMin)).toInt()
                )
                // The neighbours first, quieter; then California over them
                drawImage(image = t.surround, dstOffset = dstOffset, dstSize = dstSize)
                drawImage(image = t.state, dstOffset = dstOffset, dstSize = dstSize)
            }

            // Fills lock together seamlessly — richer gradient fills instead of flat washed-out tints
            for ((region, path) in regionPolys) {
                val isSelected = selectedRegionId == region.id
                val color = Color(android.graphics.Color.parseColor(region.colorHex))
                val bounds = path.getBounds()
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(color.copy(alpha = if (isSelected) 0.82f else tintAlpha).lighten(0.18f), color.copy(alpha = if (isSelected) 0.82f else tintAlpha)),
                        start = Offset(bounds.left, bounds.top),
                        end = Offset(bounds.right, bounds.bottom)
                    )
                )
            }

            // 3. Topographic relief — hachure ridgelines along California's actual mountain ranges,
            // giving the fills physical texture and depth instead of a flat color wash.
            val klamathCascades = listOf(
                Offset(px(-123.60f), py(41.70f)),
                Offset(px(-122.80f), py(41.20f)),
                Offset(px(-122.20f), py(40.50f)),
                Offset(px(-121.80f), py(39.80f))
            )
            val sierraNevada = listOf(
                Offset(px(-120.30f), py(39.80f)),
                Offset(px(-120.00f), py(39.00f)),
                Offset(px(-119.50f), py(38.00f)),
                Offset(px(-118.80f), py(37.00f)),
                Offset(px(-118.30f), py(36.20f)),
                Offset(px(-118.20f), py(35.30f)),
                Offset(px(-118.00f), py(34.90f))
            )
            val coastRanges = listOf(
                Offset(px(-123.40f), py(39.30f)),
                Offset(px(-122.90f), py(38.60f)),
                Offset(px(-122.30f), py(37.70f)),
                Offset(px(-121.60f), py(36.60f)),
                Offset(px(-120.90f), py(35.60f)),
                Offset(px(-120.30f), py(34.80f))
            )
            val transverseRanges = listOf(
                Offset(px(-119.60f), py(34.55f)),
                Offset(px(-118.90f), py(34.35f)),
                Offset(px(-118.10f), py(34.30f)),
                Offset(px(-117.30f), py(34.20f))
            )
            val peninsularRanges = listOf(
                Offset(px(-116.90f), py(33.70f)),
                Offset(px(-116.70f), py(33.20f)),
                Offset(px(-116.50f), py(32.70f))
            )
            if (terrain == null) {
                for (ridge in listOf(klamathCascades, sierraNevada, coastRanges, transverseRanges, peninsularRanges)) {
                    drawHachureRidge(ridge)
                }
            }

            // Clean boundary strokes — warm ink-brown for an inked, hand-drawn map feel
            for ((_, path) in regionPolys) {
                drawPath(
                    path = path,
                    color = Color(0xFF3A2A1E).copy(alpha = 0.8f),
                    style = Stroke(width = 2.4f)
                )
            }

            // Selected region highlighted stroke
            for ((region, path) in regionPolys) {
                if (selectedRegionId == region.id) {
                    drawPath(
                        path = path,
                        color = Color(0xFF2A1B12),
                        style = Stroke(width = 3f)
                    )
                    val bounds = path.getBounds()
                    drawPinMarker(
                        center = Offset(bounds.center.x, bounds.center.y),
                        color = Color(android.graphics.Color.parseColor(region.colorHex))
                    )
                }
            }

            // State lines beyond California's own edge: Oregon and Nevada share the 42°N line
            val stateLine = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
            drawLine(
                color = Color(0xFF4A3728).copy(alpha = 0.45f),
                start = Offset(px(-120.0f), py(42.0f)),
                end = Offset(px(lonMax), py(42.0f)),
                strokeWidth = 1.6f,
                pathEffect = stateLine
            )

            // Names on the regions, and on Nevada to the east
            val ink = Color(0xFF2A1B12)
            fun label(text: String, lon: Float, lat: Float, style: androidx.compose.ui.text.TextStyle) {
                val layout = textMeasurer.measure(text, style)
                drawText(
                    layout,
                    topLeft = Offset(px(lon) - layout.size.width / 2f, py(lat) - layout.size.height / 2f)
                )
            }
            // Each region's name sits on a beige pill with dark letters, so it never blends into the terrain
            val regionStyle = androidx.compose.ui.text.TextStyle(
                color = Color(0xFF1A1208),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            fun regionLabel(text: String, lon: Float, lat: Float) {
                val layout = textMeasurer.measure(text, regionStyle)
                val pad = 4.dp.toPx()
                val pillW = layout.size.width + pad * 2f
                val pillH = layout.size.height + pad * 2f
                val left = px(lon) - pillW / 2f
                val top = py(lat) - pillH / 2f
                drawRoundRect(
                    color = Color(0xFFF5EAD2).copy(alpha = 0.96f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(pillW, pillH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillH / 2f)
                )
                drawRoundRect(
                    color = Color(0xFF8B6F3E).copy(alpha = 0.55f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(pillW, pillH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillH / 2f),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawText(layout, topLeft = Offset(left + pad, top + pad))
            }
            regionLabel("Northern California", -122.2f, 40.9f)
            regionLabel("Bay Area", -122.2f, 38.45f)
            regionLabel("Central Valley", -119.6f, 36.9f)
            regionLabel("Central Coast", -121.2f, 35.4f)
            regionLabel("Los Angeles", -117.0f, 34.45f)
            regionLabel("San Diego", -116.3f, 32.95f)
            // The neighbours, quietly
            val neighbourStyle = androidx.compose.ui.text.TextStyle(
                color = Color(0xFF6B5636).copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            label("OREGON", -121.9f, 42.55f, neighbourStyle)
            label("IDAHO", -115.3f, 42.55f, neighbourStyle)
            label("MEXICO", -115.9f, 32.15f, neighbourStyle)
            label(
                "NEVADA", -116.9f, 39.3f,
                androidx.compose.ui.text.TextStyle(
                    color = Color(0xFF6B5636).copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                )
            )
        }
    }
}

package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.data.local.StateUsaTerrain
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
import io.github.genesjc.climatefinder.domain.model.GeoBounds
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember

/** Where the map's longitude and latitude fall on a canvas of [w] by [h]: one scale for both axes, centred. */
private class PickerProjection(private val view: GeoBounds, w: Float, h: Float, verticalStretch: Float) {
    private val lonSpan = (view.maxLon - view.minLon).toFloat()
    private val latSpan = (view.maxLat - view.minLat).toFloat()

    // True shape: a degree of longitude is about 0.8 of a degree of latitude
    private val k = minOf(w * 0.985f / (lonSpan * 0.8f), h * 0.985f / (latSpan * verticalStretch))
    val availW = lonSpan * 0.8f * k
    val availH = latSpan * k * verticalStretch
    val padX = (w - availW) / 2f
    val padY = (h - availH) / 2f

    fun px(lon: Float): Float = padX + ((lon - view.minLon.toFloat()) / lonSpan) * availW
    fun py(lat: Float): Float = padY + (1.0f - (lat - view.minLat.toFloat()) / latSpan) * availH
    fun lonAt(x: Float): Float = view.minLon.toFloat() + ((x - padX) / availW) * lonSpan
    fun latAt(y: Float): Float = view.minLat.toFloat() + (1f - (y - padY) / availH) * latSpan
    fun point(p: Pair<Float, Float>) = Offset(px(p.first), py(p.second))
}

private fun inPoly(pt: Offset, poly: List<Offset>): Boolean {
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

/**
 * The hand-drawn, antique-style map of the state's regions, in strict light mode. Everything specific to the state (the
 * regional polygons, which share their boundary vertices so there are no gaps, the sea, the ridges, the names) comes from
 * the state definition's picker map; this draws it and turns taps into a region.
 */
@Composable
internal fun StateUsaClassicMapCanvas(
    selectedRegionId: String,
    terrain: StateUsaTerrain?,
    onSelectRegion: (String) -> Unit,
    sizeModifier: Modifier = Modifier.height(430.dp),
    // Stretches the map vertically (1 = true shape); the picker uses a little, to be taller
    verticalStretch: Float = 1f
) {
    val stateUsa = LocalStateUsa.current
    val art = stateUsa.pickerMap
    val tintAlpha = if (terrain != null) 0.58f else 0.72f
    // The statewide pictures as Compose images, made once per picture (the data layer hands over plain bitmaps)
    val statePicture = remember(terrain) { terrain?.state?.asImageBitmap() }
    val surroundPicture = remember(terrain) { terrain?.surround?.asImageBitmap() }
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    // The names drawn on the map are read here, because the drawing code below is not composable
    val regionLabelTexts = art.regionLabels.map { stringResource(it.textRes) }
    val neighbourLabelTexts = art.neighbourLabels.map { stringResource(it.textRes) }
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
            .pointerInput(stateUsa, verticalStretch) {
                detectTapGestures { offset ->
                    val proj = PickerProjection(art.view, size.width.toFloat(), size.height.toFloat(), verticalStretch)
                    // The first region whose polygon holds the tap; a tap in the sea or beyond the border goes to the nearest centre
                    val hit = stateUsa.regionShapes.entries.firstOrNull { (_, shape) ->
                        inPoly(offset, shape.map(proj::point))
                    }?.key
                    if (hit != null) {
                        onSelectRegion(hit)
                    } else {
                        val tapLon = proj.lonAt(offset.x)
                        val tapLat = proj.latAt(offset.y)
                        val closest = stateUsa.regions.minByOrNull { r ->
                            val dLat = tapLat - r.centerLat
                            val dLon = tapLon - r.centerLon
                            dLat * dLat + dLon * dLon
                        }
                        closest?.let { onSelectRegion(it.id) }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val proj = PickerProjection(art.view, w, h, verticalStretch)
            val availW = proj.availW
            val availH = proj.availH
            fun px(lon: Float): Float = proj.px(lon)
            fun py(lat: Float): Float = proj.py(lat)

            // 1. Warm-toned dashed graticule lines (34°N, 36°N, 38°N, 40°N) — antique cartography feel
            val graticuleEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            for (glat in art.gridLatitudes) {
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
                for (p in art.seaPoints) lineTo(px(p.first), py(p.second))
                lineTo(px(art.seaBottomLon), h)
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

            // 2. The regions, seamlessly interlocking (zero interior gaps)
            val regionPolys = stateUsa.regionShapes.mapNotNull { (id, shape) ->
                stateUsa.region(id)?.let { region -> region to makePath(shape.map(proj::point)) }
            }

            // Soft glow halo behind the selected region, for a "pop on tap" feel
            for ((region, path) in regionPolys) {
                if (selectedRegionId == region.id) {
                    val color = Color(android.graphics.Color.parseColor(region.colorHex))
                    drawPath(path = path, color = color.copy(alpha = 0.30f), style = Stroke(width = 14f))
                }
            }

            // The land itself: shaded relief of the whole state under translucent region tints
            if (statePicture != null && surroundPicture != null) {
                val state = stateUsa.stateBounds
                val dstOffset = androidx.compose.ui.unit.IntOffset(px(state.minLon.toFloat()).toInt(), py(state.maxLat.toFloat()).toInt())
                val dstSize = androidx.compose.ui.unit.IntSize(
                    (availW * (state.maxLon - state.minLon).toFloat() / (art.view.maxLon - art.view.minLon).toFloat()).toInt(),
                    (availH * (state.maxLat - state.minLat).toFloat() / (art.view.maxLat - art.view.minLat).toFloat()).toInt()
                )
                // The neighbours first, quieter; then the state over them
                drawImage(image = surroundPicture, dstOffset = dstOffset, dstSize = dstSize)
                drawImage(image = statePicture, dstOffset = dstOffset, dstSize = dstSize)
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

            // 3. Topographic relief — hachure ridgelines along the state's actual mountain ranges,
            // giving the fills physical texture and depth instead of a flat color wash.
            if (terrain == null) {
                for (ridge in art.ridges) {
                    drawHachureRidge(ridge.map(proj::point))
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

            // State lines beyond the state's own edge, where it meets its neighbours
            val stateLine = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
            drawLine(
                color = Color(0xFF4A3728).copy(alpha = 0.45f),
                start = Offset(px(art.borderLine.fromLon), py(art.borderLine.lat)),
                end = Offset(px(art.view.maxLon.toFloat()), py(art.borderLine.lat)),
                strokeWidth = 1.6f,
                pathEffect = stateLine
            )

            // Names on the regions, and on the neighbours
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
            art.regionLabels.forEachIndexed { i, l -> regionLabel(regionLabelTexts[i], l.lon, l.lat) }
            // The neighbours, quietly; the one that matters most a little bigger
            val neighbourStyle = androidx.compose.ui.text.TextStyle(
                color = Color(0xFF6B5636).copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            val emphasisStyle = androidx.compose.ui.text.TextStyle(
                color = Color(0xFF6B5636).copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )
            art.neighbourLabels.forEachIndexed { i, l ->
                label(neighbourLabelTexts[i], l.lon, l.lat, if (l.emphasis) emphasisStyle else neighbourStyle)
            }
        }
    }
}

package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import io.github.genesjc.climatefinder.domain.DepthBands
import io.github.genesjc.climatefinder.domain.DepthCue
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import io.github.genesjc.climatefinder.data.local.TerrainImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.domain.model.TerrainType
import io.github.genesjc.climatefinder.ui.components.LayerColors
import io.github.genesjc.climatefinder.ui.components.hazeColor
import io.github.genesjc.climatefinder.ui.components.layerColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D

/*
 * The Elevation chart's drawing, one function for each layer of the picture, back to front: the sky, the far scenery (ranges, sea,
 * shore, boats), the weather pictures (ElevationWeather.kt), each band's ground and line, the foreground ocean and the station
 * pins. They are called from the chart's Canvas, in this order. [viewX] and [viewY] turn a chart position into a screen position at
 * the current zoom and pan; [pinBaseX] and [pinBaseY] give a station's chart position.
 */

/** The sky gradient, over the open space above the profile (or the whole chart with the bands on). Returns the sky's shape, which the far scenery is clipped to. */
internal fun DrawScope.drawElevationSky(
    bleedLeft: Float,
    bleedRight: Float,
    bleedTop: Float,
    h: Float,
    layered: Boolean,
    padding: Float,
    pinBaseX: (Int) -> Float,
    pinBaseY: (Int) -> Float,
    skyTheme: SkyAtmosphereTheme,
    sorted: List<MicroclimateStation>,
    viewX: (Float) -> Float,
    viewY: (Float) -> Float
): Path {
    val skyPath = Path().apply {
        if (layered) {
            // Layered: the ridges are drawn behind each band's ground, so the sky is the whole chart
            moveTo(bleedLeft, bleedTop)
            lineTo(bleedRight, bleedTop)
            lineTo(bleedRight, size.height * 3f)
            lineTo(bleedLeft, size.height * 3f)
        } else {
            moveTo(bleedLeft, bleedTop)
            lineTo(bleedRight, bleedTop)
            lineTo(bleedRight, viewY(pinBaseY(sorted.lastIndex.coerceAtLeast(0))))
            for (i in (sorted.size - 1) downTo 0) {
                lineTo(viewX(pinBaseX(i)), viewY(pinBaseY(i)))
            }
            lineTo(bleedLeft, viewY(pinBaseY(0)))
        }
        close()
    }

    drawPath(
        path = skyPath,
        brush = Brush.verticalGradient(
            colors = skyTheme.colors,
            startY = viewY(padding),
            endY = viewY(padding + h * 0.85f)
        )
    )
    return skyPath
}

/** One band's ground, its relief texture, haze, wash and its ridge line, for the stations [members] (left to right) of band [band]. */
internal fun DrawScope.drawBandLayer(
    members: List<Int>,
    band: Int,
    activeBand: Int?,
    angleShown: Int,
    bleedLeft: Float,
    bleedRight: Float,
    h: Float,
    landShare: Float,
    layerMix: Float,
    layered: Boolean,
    maxElevation: Float,
    padding: Float,
    pan: Offset,
    pinBaseX: (Int) -> Float,
    pinBaseY: (Int) -> Float,
    stations: List<MicroclimateStation>,
    terrain: TerrainImage?,
    terrainPicture: ImageBitmap?,
    viewX: (Float) -> Float,
    viewY: (Float) -> Float,
    zoom: Float
) {
    val terrainPath = Path()
    val linePath = Path()
    // The ground runs on below the chart's bottom edge: the ocean covers it there, except looking west
    val floor = viewY(size.height - padding) + size.height
    val leftEdge = bleedLeft
    val rightEdge = bleedRight

    // The skyline of this band's stations in their places across the view, curved through them (gentle at the turns, never
    // past the stations' own heights), straightening back to the plain profile as the bands fold away
    val skyline = members.map { Offset(viewX(pinBaseX(it)), viewY(pinBaseY(it))) }
    val firstY = skyline.first().y
    terrainPath.moveTo(leftEdge, floor)
    terrainPath.lineTo(leftEdge, firstY)
    terrainPath.lineTo(skyline.first().x, firstY)
    terrainPath.curveThrough(skyline, SKYLINE_SMOOTH * layerMix)
    linePath.moveTo(leftEdge, firstY)
    linePath.lineTo(skyline.first().x, firstY)
    linePath.curveThrough(skyline, SKYLINE_SMOOTH * layerMix)
    val lastY = skyline.last().y
    terrainPath.lineTo(rightEdge, lastY)
    linePath.lineTo(rightEdge, lastY)
    terrainPath.lineTo(rightEdge, floor)
    terrainPath.close()

    // The ground: tinted by height with the same ramp as the Topography map (data), so a given
    // height has the same colour in both views
    val groundTop = viewY(padding)
    val groundBottom = viewY(padding + h)
    drawPath(
        path = terrainPath,
        brush = Brush.verticalGradient(
            colorStops = (0..10).map { step ->
                val fraction = step / 10f
                // The height drawn at this share of the chart (clamped: above the top and below sea level)
                val ratio = (1f - (fraction - PROFILE_HEAD) / (1f - PROFILE_HEAD - SEA_FRACTION)).coerceIn(0f, 1f)
                fraction to heightTint(ratio * maxElevation)
            }.toTypedArray(),
            startY = groundTop,
            endY = groundBottom
        )
    )

    // A faint relief texture from the region's own terrain picture (decoration, not data). It
    // drifts slower than the profile when you pan and zoom, which gives the ground some depth.
    // (with the bands on, only the front layer and the active one: it is the costliest part of a frame)
    if (terrain != null && terrainPicture != null && (!layered || band == 0 || band == activeBand)) {
        val b = terrain.bounds
        val lonSpan = b.maxLon - b.minLon
        val latSpan = b.maxLat - b.minLat
        // The part of the picture that holds the stations
        val sLeft = ((stations.minOf { it.longitude } - b.minLon) / lonSpan).coerceIn(0.0, 1.0)
        val sRight = ((stations.maxOf { it.longitude } - b.minLon) / lonSpan).coerceIn(0.0, 1.0)
        val sTop = ((b.maxLat - stations.maxOf { it.latitude }) / latSpan).coerceIn(0.0, 1.0)
        val sBottom = ((b.maxLat - stations.minOf { it.latitude }) / latSpan).coerceIn(0.0, 1.0)
        val img = terrainPicture
        val srcX = (sLeft * img.width).toInt()
        val srcY = (sTop * img.height).toInt()
        val srcW = ((sRight - sLeft) * img.width).toInt().coerceAtLeast(1)
        val srcH = ((sBottom - sTop) * img.height).toInt().coerceAtLeast(1)
        val follow = 0.6f
        val scale = 1f + (zoom - 1f) * follow
        // Big enough to cover the whole chart, whatever its shape
        val cover = maxOf(size.width * 1.6f / srcW, size.height * 1.4f / srcH)
        val dstW = srcW * cover * scale
        val dstH = srcH * cover * scale
        val dstX = size.width / 2f - dstW / 2f + pan.x * follow
        val dstY = size.height / 2f - dstH / 2f + pan.y * follow
        clipPath(terrainPath) {
            drawImage(
                image = img,
                srcOffset = IntOffset(srcX, srcY),
                srcSize = IntSize(srcW, srcH),
                dstOffset = IntOffset(dstX.roundToInt(), dstY.roundToInt()),
                dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt()),
                alpha = 0.5f,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                blendMode = BlendMode.Multiply
            )
        }
    }

    // The farther layers are hazed toward the sky, and their ridge lines fainter and thinner
    // The ground's tint is the band's look on this axis: on west-east the front band wears the grey-blue haze
    // and the back band the plain ground
    val haze = hazeColor(band, angleShown)
    if (haze.alpha > 0f) drawPath(terrainPath, haze.copy(alpha = haze.alpha * layerMix))
    // The layers that do not hold the selected station step back: a wash over the ground, fainter lines
    val inactive = activeBand != null && band != activeBand
    if (inactive) drawPath(terrainPath, lerp(LayerWashColor, LayerWashColorNorth, landShare).copy(alpha = DepthBands.INACTIVE_WASH * layerMix))
    val lineFade = (1f + (DepthBands.lineFade(band) - 1f) * layerMix) *
        (if (inactive) 1f - (1f - DepthBands.INACTIVE_FADE) * layerMix else 1f)
    // The single line is always the first colour; the bands take the axis's colours as they unfold
    val lineColor = lerp(LayerColors[0], layerColor(band, angleShown), layerMix)
    // A lit edge along the profile, like the sun catching the ridge
    drawPath(
        path = linePath,
        color = Color.White.copy(alpha = 0.30f * lineFade),
        style = Stroke(width = 10f, cap = StrokeCap.Round)
    )
    // Top ridge line
    drawPath(
        path = linePath,
        color = lineColor.copy(alpha = lineFade),
        // The active layer's line is a little heavier
        style = Stroke(width = 3f * (1f - 0.15f * band) * (if (inactive) 1f else 1f + 0.25f * layerMix), cap = StrokeCap.Round)
    )
}

/** The ocean at the bottom of the chart, below sea level; it fades out as the sea moves to the horizon. */
internal fun DrawScope.drawElevationForegroundSea(h: Float, padding: Float, seaMix: Float, viewY: (Float) -> Float) {
    // The ocean: the bottom of the chart, below sea level (decoration; the heights above it are data)
    val seaY = viewY(padding + (1f - SEA_FRACTION) * h)
    val oceanBottom = maxOf(size.height, viewY(size.height - padding) + padding)
    val foregroundSea = 1f - seaMix  // fades out as the sea moves to the horizon
    if (seaY < oceanBottom && foregroundSea > 0.01f) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF7CC3E6), Color(0xFF3B8EC4), Color(0xFF1F5F99)),
                startY = seaY,
                endY = oceanBottom
            ),
            topLeft = Offset(0f, seaY),
            size = androidx.compose.ui.geometry.Size(size.width, oceanBottom - seaY),
            alpha = foregroundSea
        )
        // A light line at the waterline and a few soft wave crests
        drawLine(Color.White.copy(alpha = 0.55f * foregroundSea), Offset(0f, seaY), Offset(size.width, seaY), strokeWidth = 2.5f)
        val crest = Color.White.copy(alpha = 0.22f * foregroundSea)
        for (row in 1..3) {
            val wy = seaY + (oceanBottom - seaY) * row / 4.5f
            var wx = -(row * 37f) % 90f
            while (wx < size.width) {
                drawLine(crest, Offset(wx, wy), Offset(wx + 26f, wy), strokeWidth = 2f, cap = StrokeCap.Round)
                wx += 90f
            }
        }
    }
}

/** The station dots along the profile, the selected station's ring, the leader lines of the labelled stations and the pulse that runs up the line. */
internal fun DrawScope.drawElevationStationPins(
    activeBand: Int?,
    bandIndex: IntArray,
    depthOf: (String) -> Float?,
    dotGrowth: Float,
    layerMix: Float,
    pinBaseX: (Int) -> Float,
    pinBaseY: (Int) -> Float,
    placedIds: Set<String>,
    pulse: Animatable<Float, AnimationVector1D>,
    selectedStationId: String?,
    skyTheme: SkyAtmosphereTheme,
    sorted: List<MicroclimateStation>,
    viewX: (Float) -> Float,
    viewY: (Float) -> Float
) {
    // Station points along the slope & leader lines for landmarks
    sorted.forEachIndexed { index, station ->
        val x = viewX(pinBaseX(index))
        val y = viewY(pinBaseY(index))
        val isSelected = station.id == selectedStationId

        val isLandmark = station.id in placedIds

        val pinColor = when (station.terrain) {
            TerrainType.RIDGE -> Color(0xFF7C3AED)
            TerrainType.COASTAL -> Color(0xFF0284C7)
            TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
            TerrainType.URBAN_CANYON -> Color(0xFF475569)
        }

        // Vertical leader line for landmark cities
        if (isLandmark || isSelected) {
            drawLine(
                color = (if (isSelected) pinColor else Color.Gray).copy(alpha = 0.5f),
                start = Offset(x, y - 4f),
                end = Offset(x, y - 18f),
                strokeWidth = if (isSelected) 2f else 1.2f
            )
        }

        if (isSelected) {
            drawCircle(
                color = skyTheme.accent.copy(alpha = 0.35f),
                radius = 18f * dotGrowth,
                center = Offset(x, y)
            )
            drawCircle(
                color = skyTheme.accent,
                radius = 14f * dotGrowth,
                center = Offset(x, y),
                style = Stroke(width = 2.5f)
            )
        }

        val depth = depthOf(station.id)
        val dotCue = depth?.let { DepthCue.dotScale(it) } ?: 1f
        val bandMute = if (activeBand != null && bandIndex[index] != activeBand) 1f - (1f - DepthBands.INACTIVE_FADE) * layerMix else 1f
        val fadeCue = (depth?.let { DepthCue.alpha(it) } ?: 1f) * bandMute
        drawCircle(
            color = Color.White.copy(alpha = fadeCue),
            radius = (if (isSelected) 8f else (if (isLandmark) 6.5f else 4.5f)) * dotGrowth * dotCue,
            center = Offset(x, y)
        )
        drawCircle(
            color = (if (isSelected) skyTheme.accent else pinColor).copy(alpha = fadeCue),
            radius = (if (isSelected) 6f else (if (isLandmark) 4.8f else 3.2f)) * dotGrowth * dotCue,
            center = Offset(x, y)
        )

        // The one-off pulse: a ring that runs up the line, low dots first
        val pulseValue = pulse.value
        if (pulseValue > 0f && pulseValue < 1f) {
            val ring = (pulseValue * (sorted.size + 8f) - index) / 8f
            // With layers on, the ripple runs along the active layer only
            if (ring in 0f..1f && (activeBand == null || bandIndex[index] == activeBand)) {
                drawCircle(
                    color = Color.White.copy(alpha = (1f - ring) * 0.85f),
                    radius = (9f + 26f * ring) * dotGrowth,
                    center = Offset(x, y),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

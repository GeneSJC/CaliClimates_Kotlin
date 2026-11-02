package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import io.github.genesjc.climatefinder.domain.DepthBands
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.ui.components.HOME_TILE_BAND
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.genesjc.climatefinder.domain.model.TreeKind

/** The distant ranges, the ocean at the horizon with its boats, the shore and the trees, clipped to [skyPath]. Decoration, the same every time for a region. */
internal fun DrawScope.drawElevationFarScenery(
    bandIndex: IntArray,
    embedded: Boolean,
    h: Float,
    landMix: Float,
    padding: Float,
    pan: Offset,
    pinBaseX: (Int) -> Float,
    pinBaseY: (Int) -> Float,
    regionId: String,
    ridgeCache: RidgeHeights,
    seaMix: Float,
    skyPath: Path,
    skyTheme: SkyAtmosphereTheme,
    skyline: List<FloatArray>?,
    sorted: List<MicroclimateStation>,
    treeMix: List<TreeKind>,
    viewX: (Float) -> Float,
    viewY: (Float) -> Float,
    zoom: Float
) {
    // 1b. Distant ranges, hills and trees behind the profile, for depth. Decoration: the three
    // farthest ranges take their outline from the region's real skyline (the highest ground,
    // looking north across the region); the nearer hills and trees are made up, the same every
    // time for a region. Each layer drifts slower than the profile when you pan and zoom, and
    // hazes toward the sky colour the farther away it is.
    val horizon = skyTheme.colors.last()
    val seed = regionId.hashCode()
    // The layers keep the size they have in the regular-height chart. When the chart is taller,
    // the extra height is shared out between them (the far ranges stay near the top, the near
    // hills sink toward the ground), so nothing stretches and the gaps fill with more layers.
    val ridgeHeights = ridgeCache.get(size.width, seed, skyline)
    val ridgeH = CHART_REGULAR_HEIGHT.toPx() - padding * 2
    val extra = (h - ridgeH).coerceAtLeast(0f)
    // The ocean at the horizon, behind the nearer hills; it drifts a little with the pan and zoom, like the far ranges
    val follow = 0.30f
    val horizonBase = padding + ridgeH * 0.55f
    val horizonY = size.height / 2f + (horizonBase - size.height / 2f) * (1f + (zoom - 1f) * follow) + pan.y * follow
    val depthPx = ridgeH * 0.5f * (1f + (zoom - 1f) * follow)
    // Looking between north and west the sea is on one side and the land on the other, with a slanted coast between
    // them: the coast sweeps in from the left as the angle turns west (seaMix 0: no sea, 1: the sea all across)
    val coastSlant = size.width * COAST_SLANT
    // How far the coast travels across the screen as the view turns (a little more than the width, so the shore's wander never
    // leaves land at the right edge when looking due west)
    val coastSweep = size.width * (1f + COAST_SLANT + 0.1f)
    val coastTopX = seaMix * coastSweep
    fun coastX(y: Float) = coastTopX - coastSlant * (y - horizonY) / size.height
    // The shore is not a straight line: it wanders in and out, headlands and bays, the same for a region every time
    val shorePhase = ((seed * 37) and 0xFF) / 40f
    fun shoreX(y: Float): Float {
        val t = (y - horizonY) / size.height
        return coastX(y) + size.width * 0.035f * (sin(t * 9f + shorePhase) + 0.6f * sin(t * 21f + shorePhase * 1.7f))
    }
    val shorePoints = List(SHORE_STEPS + 1) { i ->
        val y = horizonY + size.height * 1.6f * i / SHORE_STEPS
        Offset(shoreX(y), y)
    }
    val shorePath = Path().apply {
        moveTo(shorePoints.first().x, shorePoints.first().y)
        for (k in 1 until shorePoints.size) lineTo(shorePoints[k].x, shorePoints[k].y)
    }
    val seaPath = Path().apply {
        moveTo(-size.width, horizonY)
        for (p in shorePoints) lineTo(p.x, p.y)
        lineTo(-size.width, shorePoints.last().y)
        close()
    }
    // The hills slope down into the water over this much of the width, to the waterline at the shore
    val shoreSlopeW = size.width * 0.45f
    val shoreAtHorizon = shoreX(horizonY)
    // The far ranges are drawn on the land side only; the sea side is open water
    fun withLand(clipped: Boolean, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
        if (clipped) clipPath(seaPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference, block = block) else block()
    }
    clipPath(skyPath) {
        if (seaMix > 0.01f) {
            clipPath(seaPath) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFA9D8EE), Color(0xFF5FA8D4), Color(0xFF2F7DB5)),
                        startY = horizonY,
                        endY = horizonY + depthPx
                    ),
                    topLeft = Offset(-size.width, horizonY),
                    size = androidx.compose.ui.geometry.Size(size.width * 4f, size.height * 3f)
                )
                drawLine(Color.White.copy(alpha = 0.6f), Offset(-size.width, horizonY), Offset(size.width * 3f, horizonY), strokeWidth = 2.5f)
            }
            // Sailboats on the water, so looking west is recognisable at a glance. Nobody tracks where they are, so they are
            // placed on the screen, not in the chart: always in the water that is showing, between the top of the sea and the coast
            // (the back band's ridge, the westernmost stations), above the labels, and fading out when too little water is showing,
            // so they come and go the way a label does.
            val backMembers = sorted.indices.filter { bandIndex[it] == DepthBands.COUNT - 1 }.sortedBy { pinBaseX(it) }
            if (backMembers.isNotEmpty()) {
                val ridgeXs = backMembers.map { viewX(pinBaseX(it)) }
                val ridgeYs = backMembers.map { viewY(pinBaseY(it)) }
                // The boats are part of the mural: each keeps its share of the water that is showing, so they slide across with the
                // sea as the view turns, always on the water, and drift a little with the pan. The big one is the anchor; two small
                // ones are further off, nearer the horizon.
                fun coastYAt(x: Float): Float = when {
                    x <= ridgeXs.first() -> ridgeYs.first()
                    x >= ridgeXs.last() -> ridgeYs.last()
                    else -> {
                        val k = ridgeXs.indexOfLast { it <= x }
                        val t = (x - ridgeXs[k]) / (ridgeXs[k + 1] - ridgeXs[k])
                        ridgeYs[k] + (ridgeYs[k + 1] - ridgeYs[k]) * t
                    }
                }
                // Below the weather badge and the controls, above the key and the labels near the coast
                val topSafe = ((if (embedded) HOME_TILE_BAND.value else 0f) + 110f).dp.toPx()
                val waterTop = maxOf(horizonY + 14.dp.toPx(), topSafe)
                val seed01 = ((seed * 31) and 0xFF) / 255f
                // (share of the visible water across, share of the room down, size)
                val boats = listOf(
                    Triple(0.40f + 0.15f * seed01, 0.6f, 1f),
                    Triple(0.16f + 0.08f * seed01, 0.10f, 0.5f),
                    Triple(0.74f - 0.08f * seed01, 0.26f, 0.62f)
                )
                for ((across, down, scale) in boats) {
                    val probeY = waterTop + 80.dp.toPx()
                    val sea = minOf(shoreX(probeY), size.width)
                    val coastY = coastYAt(across * sea)
                    val waterBottom = minOf(coastY - 110.dp.toPx(), size.height - 170.dp.toPx())
                    val room = waterBottom - waterTop
                    val boatY = waterTop + room * down
                    val boatX = across * minOf(shoreX(boatY), size.width) + pan.x * follow
                    // Fading in and out as the sea comes and goes at the left, and at the edges of the screen
                    val seaFade = ((minOf(shoreX(boatY), size.width) - 100.dp.toPx()) / 80.dp.toPx()).coerceIn(0f, 1f)
                    val edgeFade = (minOf(boatX, size.width - boatX) / 48.dp.toPx()).coerceIn(0f, 1f)
                    val boatAlpha = seaFade * edgeFade * (room / 60.dp.toPx()).coerceIn(0f, 1f)
                    if (boatAlpha > 0.01f) {
                        drawSailboat(
                            x = boatX,
                            y = boatY,
                            unit = 3.2.dp.toPx() * scale * (1f + (zoom - 1f) * 0.3f),
                            alpha = boatAlpha
                        )
                    }
                }
            }
        }
        RIDGE_LAYERS.forEachIndexed { layer, spec ->
            // The three far ranges give way to the sea at the horizon: they are drawn on the land side of the coast
            withLand(layer <= 2 && seaMix > 0.01f) {
            val farFade = 1f
            val steps = RIDGE_STEPS
            val startX = -size.width
            val endX = size.width * 2f
            val points = ArrayList<Offset>(steps + 1)
            val layerHeights = ridgeHeights[layer]
            for (i in 0..steps) {
                val bx = startX + (endX - startX) * i / steps
                val height = layerHeights[i]
                val by = padding + ridgeH * spec.base - ridgeH * spec.amplitude * height + spec.anchor * extra
                // The three far ranges slide across with the coast as the view turns west, the farther the slower, so the
                // scene pans as one picture with some depth
                val slide = if (layer <= 2) seaMix * coastSweep * LAND_SLIDE[layer] else 0f
                val lx = size.width / 2f + (bx - size.width / 2f) * (1f + (zoom - 1f) * spec.follow) + pan.x * spec.follow + slide
                val ly = size.height / 2f + (by - size.height / 2f) * (1f + (zoom - 1f) * spec.follow) + pan.y * spec.follow
                // Near the shore the far hills come down to the waterline, so they slope into the water
                val slopedY = if (layer <= 2 && seaMix > 0.01f) {
                    val u = ((lx - shoreAtHorizon) / shoreSlopeW).coerceIn(0f, 1f)
                    horizonY + (ly - horizonY) * (u * u * (3f - 2f * u))
                } else ly
                points.add(Offset(lx, slopedY))
            }
            val ridge = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (k in 1 until points.size) lineTo(points[k].x, points[k].y)
                lineTo(size.width * 4f, size.height * 3f)
                lineTo(-size.width * 3f, size.height * 3f)
                close()
            }
            // Darker at the ridge, hazing lighter below it, so the gap to the next layer reads as mist
            // Looking north the land takes its own colours, green and golden, in place of the sky's haze
            val skyTop = lerp(horizon, Color(0xFF334155), spec.depthShade)
            val (landTop, landLow) = LAND_RIDGE_COLORS[layer.coerceAtMost(LAND_RIDGE_COLORS.lastIndex)]
            val top = lerp(skyTop, landTop, landMix)
            val low = lerp(lerp(skyTop, horizon, 0.5f), landLow, landMix)
            val topY = points.minOf { it.y }
            drawPath(
                ridge,
                brush = Brush.verticalGradient(
                    colors = listOf(top.copy(alpha = spec.alpha * farFade), low.copy(alpha = spec.alpha * 0.7f * farFade)),
                    startY = topY,
                    endY = topY + ridgeH * 0.45f * (1f + (zoom - 1f) * spec.follow)
                )
            )
            // Looking north the middle and near far ranges carry small trees, fading in with the bands
            if ((layer == 1 || layer == 2) && landMix > 0.01f) {
                drawTrees(
                    points = points,
                    mix = treeMix,
                    sizePx = (4f + layer * 1.5f).dp.toPx() * (1f + (zoom - 1f) * spec.follow),
                    color = lerp(top, Color(0xFF0F2F2A), 0.55f).copy(alpha = (spec.alpha + 0.15f).coerceAtMost(0.95f) * landMix),
                    seed = seed + layer * 101,
                    xMin = -40f,
                    xMax = size.width + 40f
                )
            }
            if (spec.treeSizeDp > 0f) {
                val treeScale = 1f + (zoom - 1f) * spec.follow
                drawTrees(
                    points = points,
                    mix = treeMix,
                    sizePx = spec.treeSizeDp.dp.toPx() * treeScale,
                    color = lerp(top, Color(0xFF0F2F2A), 0.55f).copy(alpha = (spec.alpha + 0.15f).coerceAtMost(0.95f)),
                    seed = seed + layer * 101,
                    xMin = -40f,
                    xMax = size.width + 40f
                )
            }
            }
            if (layer == 2 && seaMix > 0.01f) {
                // The shore: shallow water and foam on the sea side, a strip of sand and a few trees on the land side
                clipPath(seaPath) {
                    drawPath(shorePath, Color(0xFFBFE6EF).copy(alpha = 0.55f), style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    drawPath(shorePath, Color.White.copy(alpha = 0.8f), style = Stroke(width = 3.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
                clipPath(seaPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                    drawPath(shorePath, Color(0xFFE6D8A6), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    for (row in 0..3) {
                        val ry = horizonY + (0.04f + 0.17f * row) * size.height
                        val rx = shoreX(ry)
                        drawTrees(
                            points = listOf(Offset(rx, ry), Offset(rx + 120.dp.toPx(), ry)),
                            mix = treeMix,
                            sizePx = (4f + 1.8f * row).dp.toPx(),
                            color = Color(0xFF3F6B3A).copy(alpha = 0.9f),
                            seed = seed + 500 + row,
                            xMin = rx + 9.dp.toPx(),
                            xMax = rx + 95.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}

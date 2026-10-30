package io.github.genesjc.climatefinder.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.math.abs
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import io.github.genesjc.climatefinder.domain.model.TreeKind

internal class Fish(
    val lengthDp: Float,
    val color: Color,
    val cycles: Int,
    val offset: Float,
    val depth: Float,
    val rightward: Boolean
)

// Three fish of different sizes, each swimming across at its own pace
internal val FISH = listOf(
    Fish(lengthDp = 16f, color = Color(0xFFFFA64D), cycles = 4, offset = 0.15f, depth = 0.28f, rightward = true),
    Fish(lengthDp = 24f, color = Color(0xFFFFE08A), cycles = 3, offset = 0.60f, depth = 0.55f, rightward = false),
    Fish(lengthDp = 34f, color = Color(0xFFD5E8F7), cycles = 2, offset = 0.35f, depth = 0.42f, rightward = true)
)

/** A little fish: an oval body, a tail that waggles with [wag] (-1 to 1) and an eye at the head. */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFish(
    cx: Float, cy: Float, length: Float, color: Color, rightward: Boolean, wag: Float
) {
    val dir = if (rightward) 1f else -1f
    val bodyW = length * 0.72f
    val bodyH = length * 0.40f
    // Body
    drawOval(
        color = color,
        topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f),
        size = androidx.compose.ui.geometry.Size(bodyW, bodyH)
    )
    // Tail, behind the body
    val tailBase = cx - dir * bodyW * 0.45f
    val tailTip = cx - dir * length * 0.5f
    val sway = wag * bodyH * 0.35f
    val tail = Path().apply {
        moveTo(tailBase, cy)
        lineTo(tailTip, cy - bodyH * 0.55f + sway)
        lineTo(tailTip, cy + bodyH * 0.55f + sway)
        close()
    }
    drawPath(tail, color = color)
    // Eye
    drawCircle(
        color = Color(0xFF0F2A44),
        radius = length * 0.045f,
        center = Offset(cx + dir * bodyW * 0.28f, cy - bodyH * 0.1f)
    )
}

/**
 * A small sailboat seen from far off, its waterline at [y] and its middle at [x]; [unit] is a fifth of a hull's length in
 * pixels and [alpha] fades it in and out. Decoration: it sits on the sea at the horizon when the chart looks west.
 */
internal fun DrawScope.drawSailboat(x: Float, y: Float, unit: Float, alpha: Float) {
    val white = Color.White
    val hullColor = Color(0xFF334155).copy(alpha = 0.9f * alpha)
    // A short light reflection under the hull
    drawLine(white.copy(alpha = 0.35f * alpha), Offset(x - 1.6f * unit, y + 1.25f * unit), Offset(x + 1.6f * unit, y + 1.25f * unit), strokeWidth = 0.2f * unit)
    // The sails: a taller main sail and a smaller one in front
    drawPath(
        Path().apply { moveTo(x + 0.25f * unit, y - 0.3f * unit); lineTo(x + 0.25f * unit, y - 5.6f * unit); lineTo(x + 2.5f * unit, y - 0.5f * unit); close() },
        white.copy(alpha = 0.95f * alpha)
    )
    drawPath(
        Path().apply { moveTo(x - 0.25f * unit, y - 0.3f * unit); lineTo(x - 0.25f * unit, y - 4.5f * unit); lineTo(x - 2.1f * unit, y - 0.55f * unit); close() },
        white.copy(alpha = 0.8f * alpha)
    )
    // The mast and the hull
    drawLine(hullColor, Offset(x, y), Offset(x, y - 5.8f * unit), strokeWidth = 0.18f * unit)
    drawPath(
        Path().apply { moveTo(x - 2.2f * unit, y); lineTo(x + 2.2f * unit, y); lineTo(x + 1.5f * unit, y + 0.85f * unit); lineTo(x - 1.5f * unit, y + 0.85f * unit); close() },
        hullColor
    )
}

/** One layer behind the profile: where it sits, how tall, how fast it drifts, how hazy, and its trees. */
internal data class RidgeLayer(
    val base: Float,        // baseline, as a fraction of the regular chart height from the top
    val amplitude: Float,   // tallest peak, as a fraction of the regular chart height
    val spread: Float,      // how many peaks fit across the chart (the made-up part of the outline)
    val follow: Float,      // how much of the pan and zoom it follows (less = farther away)
    val depthShade: Float,  // how far it is pushed toward slate from the horizon colour
    val alpha: Float,
    val anchor: Float,      // how much of the extra height of an expanded chart it moves down (0 = stays put, 1 = all of it)
    val skylineIndex: Int,  // which real skyline gives its outline (0 far, 1 middle, 2 near), or -1 for a made-up one
    val treeSizeDp: Float   // tree height, or 0 for none
)

internal val RIDGE_LAYERS = listOf(
    RidgeLayer(0.50f, 0.22f, 1.3f, 0.22f, 0.20f, 0.30f, 0.00f, 0, 0f),
    RidgeLayer(0.62f, 0.20f, 1.8f, 0.34f, 0.30f, 0.38f, 0.12f, 1, 0f),
    RidgeLayer(0.74f, 0.16f, 2.4f, 0.46f, 0.40f, 0.46f, 0.30f, 2, 0f),
    RidgeLayer(0.84f, 0.09f, 2.8f, 0.58f, 0.50f, 0.55f, 0.55f, -1, 0f),
    RidgeLayer(0.92f, 0.05f, 3.6f, 0.70f, 0.58f, 0.70f, 0.80f, -1, 11f),
    RidgeLayer(0.99f, 0.04f, 4.5f, 0.85f, 0.65f, 0.80f, 1.00f, -1, 18f)
)

/**
 * The colours of the far ranges looking north across the region, far to near, as (top of the range, its haze below): sage
 * and green hills, then golden and brown, so the land reads as land (the blue of the sky hazes the ranges everywhere else).
 * They take over from the sky's haze as the bands come on looking north.
 */
internal val LAND_RIDGE_COLORS = listOf(
    Color(0xFF8A9A7E) to Color(0xFFC9C39A),
    Color(0xFF6F8F5C) to Color(0xFFC7BE86),
    Color(0xFF5E8250) to Color(0xFFB9AD72),
    Color(0xFF7A8A4A) to Color(0xFFC4A95F),
    Color(0xFF9A8A4E) to Color(0xFFC9B070),
    Color(0xFF8A7A4A) to Color(0xFFB89A5E)
)

/**
 * The height (0 to 1) of every point of every ridge layer. These depend only on the chart's width, the region's seed
 * and its real skyline, never on zoom or pan, so they are worked out once and reused on every frame of a pan or zoom.
 */
internal class RidgeHeights {
    private var width = -1f
    private var seed = 0
    private var skyline: List<FloatArray>? = null
    private var data: Array<FloatArray> = emptyArray()

    fun get(width: Float, seed: Int, skyline: List<FloatArray>?): Array<FloatArray> {
        if (data.isNotEmpty() && width == this.width && seed == this.seed && skyline === this.skyline) return data
        data = Array(RIDGE_LAYERS.size) { layer ->
            val spec = RIDGE_LAYERS[layer]
            val realLayer = if (spec.skylineIndex >= 0) skyline?.getOrNull(spec.skylineIndex) else null
            FloatArray(RIDGE_STEPS + 1) { i ->
                val bx = -width + (width * 3f) * i / RIDGE_STEPS
                val procedural = ridgeHeight((bx / width) * spec.spread, seed, layer)
                if (realLayer != null) 0.85f * sampleSkyline(realLayer, bx / width) + 0.15f * procedural else procedural
            }
        }
        this.width = width
        this.seed = seed
        this.skyline = skyline
        return data
    }
}

internal const val RIDGE_STEPS = 110

/** The fish layer is stepped this often (about 30 a second), and one full swim across takes this long. */
internal const val FISH_STEP_MS = 33L
internal const val FISH_LOOP_SECONDS = 120f

/** A real skyline's height (0 to 1) at [t], where 0 to 1 is across the chart; it mirrors outside that. */
private fun sampleSkyline(values: FloatArray, t: Float): Float {
    val m = ((t % 2f) + 2f) % 2f
    val u = if (m <= 1f) m else 2f - m
    val pos = u * (values.size - 1)
    val i = pos.toInt().coerceAtMost(values.size - 2)
    return values[i] + (values[i + 1] - values[i]) * (pos - i)
}

/** The trees for a region the state definition does not list. */
internal val DEFAULT_TREE_MIX = listOf(TreeKind.OAK, TreeKind.CONIFER)

private fun pseudoRandom(i: Int, seed: Int): Float = (((i * 73856093) xor (seed * 19349663)) and 0xFFFF) / 65535f

/** Little trees standing along a ridgeline ([points] run left to right, evenly spaced), of the kinds in [mix]. Decoration. */
internal fun DrawScope.drawTrees(
    points: List<Offset>,
    mix: List<TreeKind>,
    sizePx: Float,
    color: Color,
    seed: Int,
    xMin: Float,
    xMax: Float
) {
    val first = points.first().x
    val last = points.last().x
    if (last <= first) return
    val step = (last - first) / (points.size - 1)
    var index = 0
    var x = first
    while (x < last) {
        val r = pseudoRandom(index, seed)
        val tx = x + (r - 0.5f) * sizePx * 0.6f
        if (tx in xMin..xMax) {
            val pos = ((tx - first) / step).coerceIn(0f, (points.size - 1).toFloat() - 0.001f)
            val k = pos.toInt()
            val y = points[k].y + (points[k + 1].y - points[k].y) * (pos - k)
            val s = sizePx * (0.7f + 0.6f * pseudoRandom(index + 7, seed))
            when (mix[(pseudoRandom(index + 13, seed) * mix.size).toInt().coerceAtMost(mix.size - 1)]) {
                TreeKind.CONIFER -> {
                    val upper = Path().apply {
                        moveTo(tx, y - s); lineTo(tx - s * 0.26f, y - s * 0.3f); lineTo(tx + s * 0.26f, y - s * 0.3f); close()
                    }
                    val lower = Path().apply {
                        moveTo(tx, y - s * 0.7f); lineTo(tx - s * 0.34f, y); lineTo(tx + s * 0.34f, y); close()
                    }
                    drawPath(upper, color)
                    drawPath(lower, color)
                }
                TreeKind.OAK -> {
                    drawLine(color, Offset(tx, y), Offset(tx, y - s * 0.3f), strokeWidth = s * 0.09f)
                    drawCircle(color, radius = s * 0.36f, center = Offset(tx, y - s * 0.6f))
                    drawCircle(color, radius = s * 0.26f, center = Offset(tx - s * 0.22f, y - s * 0.45f))
                }
                TreeKind.PALM -> {
                    val topPoint = Offset(tx + s * 0.06f, y - s * 0.85f)
                    drawLine(color, Offset(tx, y), topPoint, strokeWidth = s * 0.08f)
                    for (angle in listOf(-165f, -125f, -90f, -55f, -15f)) {
                        val rad = Math.toRadians(angle.toDouble())
                        drawLine(
                            color, topPoint,
                            Offset(topPoint.x + (kotlin.math.cos(rad) * s * 0.38f).toFloat(), topPoint.y + (kotlin.math.sin(rad) * s * 0.26f).toFloat() + s * 0.06f),
                            strokeWidth = s * 0.06f
                        )
                    }
                }
            }
        }
        index++
        x += sizePx * 0.85f
    }
}

/** A ridgeline's height from 0 to 1 at position [t]; a fixed pattern for a given region and layer. */
private fun ridgeHeight(t: Float, seed: Int, layer: Int): Float {
    val a = ((seed * 31 + layer * 17) and 0xFF) / 40f
    val b = ((seed * 57 + layer * 29) and 0xFF) / 40f
    val c = ((seed * 91 + layer * 43) and 0xFF) / 40f
    val v = 0.60f * (1f - abs(sin(t * 4.3f + a))) + 0.30f * (1f - abs(sin(t * 9.1f + b))) + 0.10f * (0.5f + 0.5f * sin(t * 21f + c))
    return v.coerceIn(0f, 1f)
}

/** The ground colour at [elevationMeters]: green lowlands, tan hills, pale high country. Matches the terrain pictures. */
internal fun heightTint(elevationMeters: Float): Color {
    val stops = listOf(
        0f to Color(0xFFE2EDD6), 300f to Color(0xFFDEE4C8), 900f to Color(0xFFD9D1B2),
        1800f to Color(0xFFCDC4AA), 3000f to Color(0xFFD6D2C8), 4200f to Color(0xFFF0F0F0)
    )
    val e = elevationMeters.coerceIn(0f, 4200f)
    val upper = stops.indexOfFirst { it.first >= e }.coerceAtLeast(1)
    val (e0, c0) = stops[upper - 1]
    val (e1, c1) = stops[upper]
    return lerp(c0, c1, if (e1 > e0) (e - e0) / (e1 - e0) else 0f)
}

// Data class for atmospheric sky gradient & weather condition above the elevation profile
internal data class SkyAtmosphereTheme(
    val colors: List<Color>,
    @StringRes val conditionRes: Int,
    val icon: String?,
    val accent: Color,
    val badgeBg: Color
)

// The wash laid over the layers that do not hold the selected station (strength: DepthBands.INACTIVE_WASH)
internal val LayerWashColor = Color(0xFFE8EEF4)

/** The wash over the layers that do not hold the selected station, looking north: a warm cream instead of the cool white-blue. */
internal val LayerWashColorNorth = Color(0xFFF1ECD8)

/** How much of the coast's sweep each of the three far ranges follows when the view turns: the farther, the slower. */
internal val LAND_SLIDE = floatArrayOf(0.55f, 0.75f, 0.9f)

/** How slanted the coast is where the sea meets the land at an angle between west and north, as a share of the chart's width. */
internal const val COAST_SLANT = 0.45f

/** How long the chart takes to fade between the single line and the layers. */
internal const val LAYERS_FADE_MS = 450

/** A little extra on the fold of an axis pivot, so the bands have settled flat before the stations are regrouped. */
internal const val AXIS_PIVOT_MARGIN_MS = 40

/** How long the labels keep their slow fade after the viewing angle last changed (ms), and how long that fade takes (seconds). */
internal const val TURNING_LINGER_MS = 1500L
internal const val TURNING_LABEL_FADE_SECONDS = 0.7f

/**
 * Which control turns the viewing angle: the quarter dial (true), with the compass at the top of the chart showing the heading, or
 * the horizontal slider with its heading strip (false). Both stay in the code while the two are compared on a device.
 */
internal const val ANGLE_ARC = true

/** The viewing angle has to move this many degrees at once (the axis switch) for the chart to fold; smaller moves (the slider) regroup at once. */
internal const val AXIS_PIVOT_JUMP_DEGREES = 30

/** The most of the chart's height the sea may take, however the chart is dragged, zoomed or re-centred (a share, 0 to 1). */
internal const val MAX_WATER_SHARE = 0.25f

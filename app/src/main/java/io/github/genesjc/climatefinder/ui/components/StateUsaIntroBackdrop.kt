package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.config.StateUsaDefinition

// Longitude degrees are narrower than latitude degrees at the middle latitudes of the USA (the same scale as the state icon).
private const val LON_SCALE = 0.8

private val INTRO_MARGIN = 18.dp
private const val INTRO_MAX_HEIGHT_SHARE = 0.62f
private val OUTLINE_LINE = Color(0xFFBAE6FD).copy(alpha = 0.55f)
private val OUTLINE_FILL = Color(0xFF1D6FC2).copy(alpha = 0.16f)
private val DOT_COLOR = Color(0xFFBAE6FD).copy(alpha = 0.9f)
private val OUTLINE_WIDTH = 2.7.dp
private val DOT_RADIUS = 2.dp

/** The state's width-to-height ratio as drawn (longitude is narrower than latitude). */
private fun StateUsaDefinition.shapeRatio(): Double {
    val points = outline.coast + outline.landBorder
    val lonSpan = (points.maxOf { it.first } - points.minOf { it.first }) * LON_SCALE
    return lonSpan / (points.maxOf { it.second } - points.minOf { it.second })
}

/**
 * How tall to draw the state on an opening view of this size: as wide as the screen less a margin on each side, but never more than
 * 62% of the height, so on a short screen it just gets smaller.
 */
internal fun StateUsaDefinition.introStateHeight(screenWidth: Dp, screenHeight: Dp): Dp =
    minOf((screenWidth - INTRO_MARGIN * 2) / shapeRatio().toFloat(), screenHeight * INTRO_MAX_HEIGHT_SHARE)

/**
 * The state's outline with a scatter of dots behind the opening view's icon: a faint blue fill, a light blue line, small light blue
 * dots. The shape is [stateHeight] tall and centred across the width, its middle [centerY] from the top; the dots that would sit under
 * the icon (a circle of [iconRadius] at the middle of the view) fade out toward its edge, so none sits on the artwork. Decorative: no
 * description, nothing loaded, and the dots are not stations.
 */
@Composable
internal fun StateUsaIntroBackdrop(
    stateUsa: StateUsaDefinition,
    stateHeight: Dp,
    centerY: Dp,
    iconRadius: Dp,
    modifier: Modifier = Modifier
) {
    val points = remember(stateUsa) { (stateUsa.outline.coast + stateUsa.outline.landBorder) }
    Canvas(modifier = modifier) {
        val lonMin = points.minOf { it.first }
        val lonMax = points.maxOf { it.first }
        val latMin = points.minOf { it.second }
        val latMax = points.maxOf { it.second }
        val height = stateHeight.toPx()
        val scale = height / (latMax - latMin)
        val width = ((lonMax - lonMin) * LON_SCALE * scale)
        val left = (size.width - width) / 2f
        val top = centerY.toPx() - height / 2f

        fun point(lon: Double, lat: Double) = Offset(
            (left + (lon - lonMin) * LON_SCALE * scale).toFloat(),
            (top + (latMax - lat) * scale).toFloat()
        )

        val path = Path().apply {
            points.forEachIndexed { i, (lon, lat) ->
                val p = point(lon, lat)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(path, color = OUTLINE_FILL)
        drawPath(path, color = OUTLINE_LINE, style = Stroke(width = OUTLINE_WIDTH.toPx(), join = StrokeJoin.Round))

        // Dots: full strength from 1.35 times the icon's radius out, fading to nothing at the icon's edge
        val middle = Offset(size.width / 2f, size.height / 2f)
        val radius = iconRadius.toPx()
        for ((lon, lat) in stateUsa.introDots) {
            val p = point(lon, lat)
            val distance = (p - middle).getDistance()
            val strength = ((distance - radius) / (radius * 0.35f)).coerceIn(0f, 1f)
            if (strength > 0f) drawCircle(DOT_COLOR.copy(alpha = DOT_COLOR.alpha * strength), radius = DOT_RADIUS.toPx(), center = p)
        }
    }
}

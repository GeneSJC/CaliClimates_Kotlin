package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

internal fun Color.lighten(fraction: Float): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha
)

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPinMarker(center: Offset, color: Color) {
    drawCircle(color = Color(0xFF2A1B12).copy(alpha = 0.35f), radius = 11f, center = center)
    drawCircle(color = Color.White, radius = 8.5f, center = center)
    drawCircle(color = color, radius = 6f, center = center)
}

/**
 * Draws a hand-drawn "hachure" ridgeline along [points]: short ticks perpendicular to the
 * ridge, dark on one side and light on the other, mimicking relief-shaded mountains on an
 * antique topographic map. Ticks taper in near both ends of the ridge for a natural point.
 */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHachureRidge(
    points: List<Offset>,
    tickLength: Float = 11f,
    spacing: Float = 8f,
    darkColor: Color = Color(0xFF4A3728).copy(alpha = 0.42f),
    lightColor: Color = Color(0xFFFFF6DE).copy(alpha = 0.55f)
) {
    if (points.size < 2) return
    val segmentLengths = FloatArray(points.size - 1) { i ->
        val dx = points[i + 1].x - points[i].x
        val dy = points[i + 1].y - points[i].y
        kotlin.math.hypot(dx, dy)
    }
    val totalLength = segmentLengths.sum()
    if (totalLength < 1f) return

    var traveled = 0f
    var distanceSinceTick = 0f
    for (i in segmentLengths.indices) {
        val segLen = segmentLengths[i]
        if (segLen < 0.5f) continue
        val a = points[i]
        val b = points[i + 1]
        val dirX = (b.x - a.x) / segLen
        val dirY = (b.y - a.y) / segLen
        val normalX = -dirY
        val normalY = dirX

        var distanceInSegment = spacing - distanceSinceTick
        while (distanceInSegment < segLen) {
            val tickCenter = Offset(a.x + dirX * distanceInSegment, a.y + dirY * distanceInSegment)
            val positionRatio = (traveled + distanceInSegment) / totalLength
            // Taper ticks near both ends so the ridge fades in/out naturally
            val taper = (kotlin.math.sin(positionRatio * Math.PI).toFloat()).coerceIn(0.35f, 1f)
            val tLen = tickLength * taper
            drawLine(
                color = darkColor,
                start = tickCenter,
                end = Offset(tickCenter.x + normalX * tLen, tickCenter.y + normalY * tLen),
                strokeWidth = 1.4f
            )
            drawLine(
                color = lightColor,
                start = tickCenter,
                end = Offset(tickCenter.x - normalX * tLen * 0.6f, tickCenter.y - normalY * tLen * 0.6f),
                strokeWidth = 1.1f
            )
            distanceInSegment += spacing
        }
        distanceSinceTick = segLen - (distanceInSegment - spacing)
        traveled += segLen
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCompassRose(center: Offset, radius: Float, color: Color) {
    val points = mutableListOf<Offset>()
    val spikes = 8
    for (i in 0 until spikes * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.38f
        val angle = (Math.PI * i / spikes) - Math.PI / 2
        points.add(Offset(center.x + (r * kotlin.math.cos(angle)).toFloat(), center.y + (r * kotlin.math.sin(angle)).toFloat()))
    }
    val star = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        close()
    }
    drawPath(star, color = color.copy(alpha = 0.35f))
    drawCircle(color = color.copy(alpha = 0.5f), radius = radius * 0.14f, center = center)
}

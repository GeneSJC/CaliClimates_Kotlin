package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.domain.model.StateUsaOutline
import io.github.genesjc.climatefinder.domain.model.TerrainType
import io.github.genesjc.climatefinder.ui.components.ms

/** The dot colour for a terrain type; the same colours as the legend. */
internal fun pinColor(terrain: TerrainType): Color = when (terrain) {
    TerrainType.COASTAL -> Color(0xFF0284C7)
    TerrainType.RIDGE -> Color(0xFF7C3AED)
    TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
    TerrainType.URBAN_CANYON -> Color(0xFF475569)
}

/** One station dot; the selected one gets a halo and a ring, and is slightly larger. */
internal fun DrawScope.drawStationPin(x: Float, y: Float, color: Color, dotScale: Float, selected: Boolean) {
    if (selected) {
        drawCircle(color = color.copy(alpha = 0.35f), radius = 20f * dotScale, center = Offset(x, y))
        drawCircle(
            color = color,
            radius = 16f * dotScale,
            center = Offset(x, y),
            style = Stroke(width = 3f * dotScale)
        )
    }
    drawCircle(color = Color.White, radius = if (selected) 9f * dotScale else 7f * dotScale, center = Offset(x, y))
    drawCircle(color = color, radius = if (selected) 7f * dotScale else 5.5f * dotScale, center = Offset(x, y))
}

// Maximum number of place labels drawn on the neutral base map
internal const val MAP_LABEL_COUNT = 6

/** Plain base map with no terrain artwork: land, ocean, state border, coast and a lat/lon grid. */
internal fun DrawScope.drawNeutralBase(
    outline: StateUsaOutline,
    minLon: Double,
    maxLon: Double,
    minLat: Double,
    maxLat: Double,
    padding: Float,
    mapX: (Double) -> Float,
    mapY: (Double) -> Float,
    zoom: Float,
    textMeasurer: TextMeasurer
) {
    val far = 5000f
    val coast = outline.coast
    val border = outline.landBorder

    // Land
    drawRect(color = Color(0xFFF1F6EC))

    // Outside the state (its neighbours): everything past the land border
    val outside = Path().apply {
        moveTo(mapX(border.first().first), mapY(border.first().second))
        for (i in 1 until border.size) lineTo(mapX(border[i].first), mapY(border[i].second))
        lineTo(mapX(border.last().first), -far)
        lineTo(far, -far)
        lineTo(far, mapY(border.first().second))
        close()
    }
    drawPath(outside, color = Color(0xFFE8E8E1))

    // Pacific Ocean: everything west of the coast
    val ocean = Path().apply {
        moveTo(mapX(coast.first().first), -far)
        for (point in coast) lineTo(mapX(point.first), mapY(point.second))
        lineTo(mapX(coast.last().first), far)
        lineTo(-far, far)
        lineTo(-far, -far)
        close()
    }
    drawPath(ocean, color = Color(0xFFDCEBF4))

    drawGraticule(minLon, maxLon, minLat, maxLat, padding, mapX, mapY, zoom, textMeasurer)

    // Border (dashed) and coast
    val borderPath = Path().apply {
        moveTo(mapX(border.first().first), mapY(border.first().second))
        for (i in 1 until border.size) lineTo(mapX(border[i].first), mapY(border[i].second))
    }
    drawPath(
        borderPath,
        color = Color(0xFF94A3B8),
        style = Stroke(width = 1.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    )
    val coastPath = Path().apply {
        moveTo(mapX(coast.first().first), mapY(coast.first().second))
        for (i in 1 until coast.size) lineTo(mapX(coast[i].first), mapY(coast[i].second))
    }
    drawPath(coastPath, color = Color(0xFF7FA7C4), style = Stroke(width = 2.6f, cap = StrokeCap.Round))
}

/** A light latitude / longitude grid with degree labels, drawn over the base. */
internal fun DrawScope.drawGraticule(
    minLon: Double,
    maxLon: Double,
    minLat: Double,
    maxLat: Double,
    padding: Float,
    mapX: (Double) -> Float,
    mapY: (Double) -> Float,
    zoom: Float,
    textMeasurer: TextMeasurer
) {
    // Latitude / longitude grid with degree labels
    // Steps chosen so neighboring labels stay at least ~64dp (longitude) and ~44dp (latitude) apart
    fun niceStep(spanDegrees: Double, lengthPx: Float, minGapPx: Float): Double {
        val steps = listOf(0.05, 0.1, 0.25, 0.5, 1.0, 2.0, 5.0)
        return steps.firstOrNull { it / spanDegrees * lengthPx >= minGapPx } ?: steps.last()
    }
    val lonStep = niceStep((maxLon - minLon) / zoom, size.width - padding * 2, 64.dp.toPx())
    val latStep = niceStep((maxLat - minLat) / zoom, size.height - padding * 2, 44.dp.toPx())
    fun degrees(value: Double): String = "%.2f".format(value).trimEnd('0').trimEnd('.')
    val labelStyle = TextStyle(fontSize = ms(8f), color = Color(0xFF64748B))
    var lon = ceil(minLon / lonStep) * lonStep
    while (lon <= maxLon) {
        val x = mapX(lon)
        drawLine(Color.DarkGray.copy(alpha = 0.10f), Offset(x, padding), Offset(x, size.height - padding), 1f)
        // Skip labels under the scale bar (bottom-left)
        if (x > padding + 120.dp.toPx()) {
            val label = textMeasurer.measure("${degrees(abs(lon))}\u00B0W", labelStyle)
            drawText(label, topLeft = Offset(x + 2f, size.height - padding - label.size.height))
        }
        lon += lonStep
    }
    var lat = ceil(minLat / latStep) * latStep
    while (lat <= maxLat) {
        val y = mapY(lat)
        drawLine(Color.DarkGray.copy(alpha = 0.10f), Offset(padding, y), Offset(size.width - padding, y), 1f)
        // Skip labels under the compass (top-left)
        if (y > padding + 34.dp.toPx()) {
            val label = textMeasurer.measure("${degrees(lat)}\u00B0N", labelStyle)
            drawText(label, topLeft = Offset(padding + 3f, y - label.size.height - 1f))
        }
        lat += latStep
    }
}

/**
 * Picks about six stations spread across the map for place labels. Airport stations (K-prefixed)
 * are preferred because they are recognizable cities.
 */
internal fun pickMapLandmarks(stations: List<MicroclimateStation>, count: Int = MAP_LABEL_COUNT * 2): List<MicroclimateStation> {
    if (stations.size <= count) return stations
    val midLat = stations.map { it.latitude }.average()
    val lonScale = cos(midLat * PI / 180)
    fun dist(a: MicroclimateStation, b: MicroclimateStation): Double {
        val dx = (a.longitude - b.longitude) * lonScale
        val dy = a.latitude - b.latitude
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    fun bonus(s: MicroclimateStation) = if (s.id.startsWith("K")) 0.4 else 0.0
    val centerLon = stations.map { it.longitude }.average()
    val seed = stations.minByOrNull { (abs(it.latitude - midLat) + abs(it.longitude - centerLon)) - bonus(it) }
        ?: return stations.take(count)
    val chosen = mutableListOf(seed)
    while (chosen.size < count) {
        val next = stations
            .filter { it !in chosen }
            .maxByOrNull { candidate -> chosen.minOf { dist(candidate, it) } + bonus(candidate) }
            ?: break
        chosen.add(next)
    }
    return chosen
}

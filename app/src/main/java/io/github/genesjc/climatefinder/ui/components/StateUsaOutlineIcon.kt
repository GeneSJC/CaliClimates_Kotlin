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
import androidx.compose.ui.graphics.drawscope.clipPath
import io.github.genesjc.climatefinder.config.StateUsaDefinition
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa

// Longitude degrees are narrower than latitude degrees at the middle latitudes of the USA.
private const val LON_SCALE = 0.8f

/** The state's border as (lon, lat) vertices: the coast, then the land border back round to the start. */
private fun StateUsaDefinition.outlinePoints(): List<Pair<Float, Float>> =
    (outline.coast + outline.landBorder).map { it.first.toFloat() to it.second.toFloat() }

/** Small silhouette of the state, drawn from the same coordinates as the Regional Sections map. */
@Composable
fun StateUsaOutlineIcon(
    fill: Color,
    stroke: Color,
    modifier: Modifier = Modifier,
    /** When given, the icon is filled region by region in these colours, like the picker's map. */
    regionColors: Map<String, Color> = emptyMap(),
    /** The current region, drawn at full strength; the others are softer. */
    selectedRegionId: String? = null
) {
    val stateUsa = LocalStateUsa.current
    val points = remember(stateUsa) { stateUsa.outlinePoints() }
    Canvas(modifier = modifier) {
        val lonMin = points.minOf { it.first }
        val lonMax = points.maxOf { it.first }
        val latMin = points.minOf { it.second }
        val latMax = points.maxOf { it.second }
        val lonSpan = (lonMax - lonMin) * LON_SCALE
        val latSpan = latMax - latMin
        val scale = minOf(size.width / lonSpan, size.height / latSpan)
        val offsetX = (size.width - lonSpan * scale) / 2f
        val offsetY = (size.height - latSpan * scale) / 2f

        fun point(lon: Float, lat: Float) = Offset(
            offsetX + (lon - lonMin) * LON_SCALE * scale,
            offsetY + (latMax - lat) * scale
        )

        val path = Path().apply {
            points.forEachIndexed { i, (lon, lat) ->
                val p = point(lon, lat)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        if (regionColors.isEmpty()) {
            drawPath(path, color = fill)
        } else {
            // The state's shape clips the regions, so the coast stays as in the outline
            clipPath(path) {
                for ((id, pts) in stateUsa.regionShapes) {
                    val color = regionColors[id] ?: continue
                    val p = Path().apply {
                        pts.forEachIndexed { i, (lon, lat) ->
                            val o = point(lon, lat)
                            if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                        }
                        close()
                    }
                    drawPath(p, color = color.copy(alpha = if (id == selectedRegionId || selectedRegionId == null) 1f else 0.55f))
                    drawPath(p, color = Color(0xFF4A3728).copy(alpha = 0.5f), style = Stroke(width = size.minDimension * 0.012f))
                }
            }
        }
        drawPath(
            path,
            color = stroke,
            style = Stroke(width = size.minDimension * 0.04f, join = StrokeJoin.Round)
        )
    }
}

/**
 * Just the selected region's shape, filled in its picker colour and outlined, scaled up to fill the space (the
 * state's outline is not drawn). The region's polygon is clipped by the state's coast, so the shoreline matches
 * the full-state icon.
 */
@Composable
fun RegionShapeIcon(
    regionId: String,
    fill: Color,
    stroke: Color,
    modifier: Modifier = Modifier
) {
    val stateUsa = LocalStateUsa.current
    val outlinePoints = remember(stateUsa) { stateUsa.outlinePoints() }
    Canvas(modifier = modifier) {
        val pts = stateUsa.regionShapes[regionId] ?: return@Canvas
        val minLon = pts.minOf { it.first }
        val maxLon = pts.maxOf { it.first }
        val minLat = pts.minOf { it.second }
        val maxLat = pts.maxOf { it.second }
        val lonSpan = ((maxLon - minLon) * LON_SCALE).coerceAtLeast(0.01f)
        val latSpan = (maxLat - minLat).coerceAtLeast(0.01f)
        // Room for the outline's width, so it is not cut at the edges
        val inset = size.minDimension * 0.05f
        val availW = size.width - inset * 2f
        val availH = size.height - inset * 2f
        val scale = minOf(availW / lonSpan, availH / latSpan)
        val offsetX = inset + (availW - lonSpan * scale) / 2f
        val offsetY = inset + (availH - latSpan * scale) / 2f
        fun point(lon: Float, lat: Float) = Offset(
            offsetX + (lon - minLon) * LON_SCALE * scale,
            offsetY + (maxLat - lat) * scale
        )
        val region = Path().apply {
            pts.forEachIndexed { i, (lon, lat) ->
                val p = point(lon, lat)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        val state = Path().apply {
            outlinePoints.forEachIndexed { i, (lon, lat) ->
                val p = point(lon, lat)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        clipPath(state) { drawPath(region, color = fill) }
        drawPath(region, color = stroke, style = Stroke(width = size.minDimension * 0.05f, join = StrokeJoin.Round))
    }
}

package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath

// California border as (lon, lat) vertices: Pacific coast north to south, the Mexico
// and Colorado River edge, then the Nevada diagonal back up to Oregon.
private val CA_OUTLINE_POINTS = listOf(
    -124.21f to 42.00f, -124.41f to 40.44f, -123.65f to 38.80f, -123.00f to 38.00f,
    -122.51f to 37.77f, -122.20f to 37.00f, -121.85f to 36.80f, -121.90f to 36.30f,
    -120.85f to 35.30f, -120.47f to 34.45f, -119.70f to 34.40f, -119.10f to 34.10f,
    -118.85f to 34.02f, -118.40f to 33.74f, -117.60f to 33.38f, -117.25f to 32.72f,
    -117.12f to 32.53f, -114.72f to 32.72f, -114.63f to 35.00f, -120.00f to 39.00f,
    -120.00f to 42.00f
)

private const val CA_LON_MIN = -124.41f
private const val CA_LON_MAX = -114.63f
private const val CA_LAT_MIN = 32.53f
private const val CA_LAT_MAX = 42.00f

// Longitude degrees are narrower than latitude degrees at California's latitude.
private const val CA_LON_SCALE = 0.8f

/** Small California silhouette drawn from the same coordinates as the Regional Sections map. */
@Composable
fun CaliforniaOutlineIcon(
    fill: Color,
    stroke: Color,
    modifier: Modifier = Modifier,
    /** When given, the icon is filled region by region in these colours, like the picker's map. */
    regionColors: Map<String, Color> = emptyMap(),
    /** The current region, drawn at full strength; the others are softer. */
    selectedRegionId: String? = null
) {
    Canvas(modifier = modifier) {
        val lonSpan = (CA_LON_MAX - CA_LON_MIN) * CA_LON_SCALE
        val latSpan = CA_LAT_MAX - CA_LAT_MIN
        val scale = minOf(size.width / lonSpan, size.height / latSpan)
        val offsetX = (size.width - lonSpan * scale) / 2f
        val offsetY = (size.height - latSpan * scale) / 2f

        fun point(lon: Float, lat: Float) = Offset(
            offsetX + (lon - CA_LON_MIN) * CA_LON_SCALE * scale,
            offsetY + (CA_LAT_MAX - lat) * scale
        )

        val path = Path().apply {
            CA_OUTLINE_POINTS.forEachIndexed { i, (lon, lat) ->
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
                for ((id, pts) in CALIFORNIA_REGION_SHAPES) {
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
    Canvas(modifier = modifier) {
        val pts = CALIFORNIA_REGION_SHAPES[regionId] ?: return@Canvas
        val minLon = pts.minOf { it.first }
        val maxLon = pts.maxOf { it.first }
        val minLat = pts.minOf { it.second }
        val maxLat = pts.maxOf { it.second }
        val lonSpan = ((maxLon - minLon) * CA_LON_SCALE).coerceAtLeast(0.01f)
        val latSpan = (maxLat - minLat).coerceAtLeast(0.01f)
        // Room for the outline's width, so it is not cut at the edges
        val inset = size.minDimension * 0.05f
        val availW = size.width - inset * 2f
        val availH = size.height - inset * 2f
        val scale = minOf(availW / lonSpan, availH / latSpan)
        val offsetX = inset + (availW - lonSpan * scale) / 2f
        val offsetY = inset + (availH - latSpan * scale) / 2f
        fun point(lon: Float, lat: Float) = Offset(
            offsetX + (lon - minLon) * CA_LON_SCALE * scale,
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
            CA_OUTLINE_POINTS.forEachIndexed { i, (lon, lat) ->
                val p = point(lon, lat)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        clipPath(state) { drawPath(region, color = fill) }
        drawPath(region, color = stroke, style = Stroke(width = size.minDimension * 0.05f, join = StrokeJoin.Round))
    }
}

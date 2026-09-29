package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

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
    modifier: Modifier = Modifier
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
        drawPath(path, color = fill)
        drawPath(
            path,
            color = stroke,
            style = Stroke(width = size.minDimension * 0.04f, join = StrokeJoin.Round)
        )
    }
}

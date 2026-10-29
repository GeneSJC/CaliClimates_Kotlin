package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.DepthBands

/** The haze laid over each layer's ground in the Elevation Bands view, nearest first (the nearest has none); the strength is [DepthBands.haze]. */
val LayerHazeColors = listOf(Color.Transparent, Color(0xFF5FA8A0), Color(0xFF7B8CD6))

private val FOOTHILL_GROUND = Color(0xFFD9D1B2)

/** What each layer's ground looks like on the chart (the foothill tint under the layer's haze), so its key matches it. */
val LayerGroundColors = List(DepthBands.COUNT) { band ->
    lerp(FOOTHILL_GROUND, LayerHazeColors[band], DepthBands.haze(band))
}

/** The key's size (dp), which the chart also keeps its labels clear of: three segments in a bar. */
const val LAYER_LEGEND_WIDTH_DP = 3 * 64f + 2 * 2f + 6f
const val LAYER_LEGEND_HEIGHT_DP = 40f

/**
 * The key to the Elevation Bands view, and a way to move between the bands: a bar of three segments, North, Central
 * and South from left to right (the back ridge first), each in its band's colour. The active band's segment, the one
 * holding the selected station, is filled with that band's ground colour on the chart and outlined in its line colour. Tapping another band's segment calls [onPick] with that band; a band with
 * no stations ([hasStations] false) is greyed out. It floats in one place whatever the pan and zoom, and [alpha]
 * fades it in and out with the view.
 */
@Composable
fun LayerLegend(
    activeBand: Int?,
    hasStations: List<Boolean>,
    alpha: Float,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val names = listOf(R.string.elevation_band_south, R.string.elevation_band_central, R.string.elevation_band_north)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        shadowElevation = 2.dp,
        modifier = modifier.alpha(alpha)
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (band in names.indices.reversed()) {
                val color = LayerColors[band]
                val active = band == activeBand
                val enabled = hasStations.getOrElse(band) { false }
                val name = stringResource(names[band])
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(64.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .border(if (active) 1.5.dp else 0.dp, if (active) color else Color.Transparent, RoundedCornerShape(9.dp))
                        .background(if (active) LayerGroundColors[band] else Color.Transparent)
                        .clickable(
                            enabled = enabled && !active,
                            onClickLabel = stringResource(R.string.elevation_band_pick, name),
                            role = Role.Button,
                            onClick = { onPick(band) }
                        )
                ) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Color(0xFF1E293B) else color.copy(alpha = if (enabled) 0.95f else 0.35f)
                    )
                }
            }
        }
    }
}

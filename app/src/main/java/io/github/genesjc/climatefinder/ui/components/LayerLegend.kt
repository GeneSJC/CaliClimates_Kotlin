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
import io.github.genesjc.climatefinder.domain.BandAxis
import io.github.genesjc.climatefinder.domain.DepthBands

/** The haze laid over each layer's ground in the Elevation Bands view, nearest first (the nearest has none); the strength is [DepthBands.haze]. */
val LayerHazeColors = listOf(Color.Transparent, Color(0xFF5FA8A0), Color(0xFF7B8CD6))

private val FOOTHILL_GROUND = Color(0xFFD9D1B2)

/** The same tone as each layer's ground on the chart (the foothill tint under the layer's haze), taken darker so white text reads on it. */
val LayerKeyColors = List(DepthBands.COUNT) { band ->
    lerp(lerp(FOOTHILL_GROUND, LayerHazeColors[band], DepthBands.haze(band)), Color.Black, 0.5f)
}

/** The axis switch at the left of the key (dp). */
private const val AXIS_CELL_WIDTH_DP = 40f

/** The key's height (dp), which the chart also keeps its labels clear of. */
const val LAYER_LEGEND_HEIGHT_DP = 40f

/** The key's width (dp), which the chart also keeps its labels clear of: three segments in a bar, and the axis switch when it shows. */
fun layerLegendWidthDp(withAxisSwitch: Boolean): Float =
    3 * 64f + 2 * 2f + 6f + (if (withAxisSwitch) AXIS_CELL_WIDTH_DP + 2f else 0f)

/**
 * The key to the bands (shown in the single-line view too, where the tags carry the band's colour as a stripe), and a way to move between them: a bar of three segments, from left to right the back ridge first
 * (North, Central and South looking north; West, Central and East looking west), each in its band's colour. With the bands on
 * ([showAxisSwitch]) a small cell at the left switches the axis ([onToggleAxis]); it reads the axis a tap would switch to. With
 * the single line showing there is nothing for the axis to change, so the cell is left out. The active band's segment, the one
 * holding the selected station, is filled with a darker tone of that band's ground colour on the chart, with white text, and outlined in its line colour. Tapping another band's segment calls [onPick] with that band; a band with
 * no stations ([hasStations] false) is greyed out. It floats in one place whatever the pan and zoom, and [alpha]
 * fades it in and out with the view.
 */
@Composable
fun LayerLegend(
    axis: BandAxis,
    onToggleAxis: () -> Unit,
    showAxisSwitch: Boolean,
    activeBand: Int?,
    hasStations: List<Boolean>,
    alpha: Float,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** The axis the chart is being switched to, when it differs from [axis] (during a pivot); the switch cell follows it. */
    requestedAxis: BandAxis = axis
) {
    // Front first: band 0 is the nearest ridge
    val names = when (axis) {
        BandAxis.NORTH_SOUTH -> listOf(R.string.elevation_band_south, R.string.elevation_band_central, R.string.elevation_band_north)
        BandAxis.WEST_EAST -> listOf(R.string.elevation_band_east, R.string.elevation_band_central, R.string.elevation_band_west)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        // Dark and a little see-through, like the weather badge and the notice, so the bar sits back in the chart
        color = Color(0xE61E293B),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 2.dp,
        modifier = modifier.alpha(alpha)
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (showAxisSwitch) AxisCell(requestedAxis, onToggleAxis)
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
                        .background(if (active) LayerKeyColors[band] else Color.Transparent)
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
                        color = if (active) Color.White else lerp(color, Color.White, 0.55f).copy(alpha = if (enabled) 1f else 0.4f)
                    )
                }
            }
        }
    }
}

/** The switch between the two axes: it shows where a tap would go (W-E while the bands run north to south), as the layers button shows its action. */
@Composable
private fun AxisCell(axis: BandAxis, onToggle: () -> Unit) {
    val target = axis.other()
    val label = stringResource(if (target == BandAxis.WEST_EAST) R.string.elevation_axis_cell_west_east else R.string.elevation_axis_cell_north_south)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(AXIS_CELL_WIDTH_DP.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(
                onClickLabel = stringResource(
                    if (target == BandAxis.WEST_EAST) R.string.elevation_axis_to_west_east else R.string.elevation_axis_to_north_south
                ),
                role = Role.Button,
                onClick = onToggle
            )
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
    }
}

package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R

/**
 * The key to the Elevation Bands view: a chip per band in the band's own colour, the northernmost (the back ridge) at
 * the top. The chip of the active band, the one holding the selected station, is filled; the others are outlined and
 * faded. It floats in one place whatever the pan and zoom. [alpha] fades it in and out with the view.
 */
@Composable
fun LayerLegend(activeBand: Int?, alpha: Float, modifier: Modifier = Modifier) {
    val names = listOf(R.string.elevation_band_south, R.string.elevation_band_central, R.string.elevation_band_north)
    Column(
        modifier = modifier.alpha(alpha),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.End
    ) {
        for (band in names.indices.reversed()) {
            val color = LayerColors[band]
            val active = band == activeBand
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (active) color else Color.White.copy(alpha = 0.85f),
                border = BorderStroke(1.5.dp, color.copy(alpha = if (active) 1f else 0.55f))
            ) {
                Text(
                    text = stringResource(names[band]),
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) Color.White else color.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}

package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.R

/** The Elevation chart's button for its layers view: three stacked ridges, lit while the layers are [on]. */
@Composable
fun LayersToggleButton(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val ink = if (on) Color.White else Color(0xFF0284C7)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (on) Color(0xFF0284C7) else Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .size(32.dp)
            .clickable(
                onClickLabel = stringResource(if (on) R.string.elevation_layers_hide else R.string.elevation_layers_show),
                role = Role.Button,
                onClick = onToggle
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 8.dp)) {
            val stroke = 2.2.dp.toPx()
            val w = size.width
            val h = size.height
            // Three lines, the nearest (bottom) longest and solid, the farthest (top) short and faint
            for (row in 0..2) {
                val y = h * (0.1f + 0.4f * row)
                val inset = w * 0.2f * (2 - row)
                drawLine(
                    ink.copy(alpha = 0.45f + 0.275f * row),
                    Offset(inset, y), Offset(w - inset, y),
                    strokeWidth = stroke, cap = StrokeCap.Round
                )
            }
        }
    }
}

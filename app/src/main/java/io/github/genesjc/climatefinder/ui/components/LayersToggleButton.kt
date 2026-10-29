package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.R

/** The line colour of each layer, nearest (front) first: the purple of the single line, then teal, then blue. */
val LayerColors = listOf(Color(0xFF7C3AED), Color(0xFF0D9488), Color(0xFF2563EB))

/**
 * The Elevation chart's button for its layers view. It shows the view the chart is in: one vertical bar for the
 * single line, three stacked ridges in the layers' colours for the Elevation Bands (the farthest at the top).
 */
@Composable
fun LayersToggleButton(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(if (on) 2.dp else 1.dp, if (on) LayerColors[1] else Color(0xFFCBD5E1)),
        modifier = modifier
            .size(36.dp)
            .clickable(
                onClickLabel = stringResource(if (on) R.string.elevation_layers_hide else R.string.elevation_layers_show),
                role = Role.Button,
                onClick = onToggle
            )
    ) {
        Crossfade(targetState = on, label = "layersIcon") { layers ->
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                val stroke = 2.4.dp.toPx()
                val w = size.width
                val h = size.height
                if (layers) {
                    // Three parallel ridges, each a jagged slope from bottom left to top right, like the chart: the
                    // farthest (top) behind, the nearest (bottom) in front
                    val ridge = listOf(0f to 1f, 0.28f to 0.7f, 0.46f to 0.8f, 0.76f to 0.34f, 1f to 0.18f)
                    for (band in 2 downTo 0) {
                        val top = h * 0.2f * (2 - band)
                        val span = h * 0.6f
                        val path = Path()
                        ridge.forEachIndexed { i, (x, t) ->
                            if (i == 0) path.moveTo(x * w, top + t * span) else path.lineTo(x * w, top + t * span)
                        }
                        drawPath(path, LayerColors[band], style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                } else {
                    drawLine(LayerColors[0], Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = stroke, cap = StrokeCap.Round)
                }
            }
        }
    }
}

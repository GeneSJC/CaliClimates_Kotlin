package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
 * The Elevation chart's button for switching views. It shows the view a tap switches to: while the chart is the single
 * line ([on] false) it shows the Elevation Bands icon (three white ridges on dark); while the bands are on it shows the
 * single ridge (purple on white).
 */
@Composable
fun LayersToggleButton(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val background by animateColorAsState(if (on) Color.White.copy(alpha = 0.92f) else Color(0xFF1E293B), label = "layersButtonColor")
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = background,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .size(36.dp)
            .clickable(
                onClickLabel = stringResource(if (on) R.string.elevation_layers_hide else R.string.elevation_layers_show),
                role = Role.Button,
                onClick = onToggle
            )
    ) {
        // The icon of the view a tap goes to: three ridges (white on the dark button) while on the single line
        Crossfade(targetState = on, label = "layersIcon") { bandsOn ->
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp)) {
                val stroke = 2.2.dp.toPx()
                val w = size.width
                val h = size.height
                // A jagged slope from bottom left to top right, like a ridge on the chart
                val ridge = listOf(0f to 1f, 0.28f to 0.66f, 0.46f to 0.8f, 0.76f to 0.3f, 1f to 0.1f)
                fun drawRidge(top: Float, span: Float, color: Color) {
                    val path = Path()
                    ridge.forEachIndexed { i, (x, t) ->
                        if (i == 0) path.moveTo(x * w, top + t * span) else path.lineTo(x * w, top + t * span)
                    }
                    drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                val span = h * 0.44f
                if (bandsOn) {
                    // The single ridge, alone in the middle
                    drawRidge(top = (h - span) / 2f, span = span, color = LayerColors[0])
                } else {
                    // Three of them, well apart
                    for (band in 2 downTo 0) drawRidge(top = h * 0.28f * (2 - band), span = span, color = Color.White)
                }
            }
        }
    }
}

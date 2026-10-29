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
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** The line colour of each layer, nearest (front) first: the purple of the single line, then teal, then blue. */
val LayerColors = listOf(Color(0xFF7C3AED), Color(0xFF0D9488), Color(0xFF2563EB))

/** The dark green of the Elevation Bands controls and its notice. */
val BandsGreen = Color(0xFF14532D)

/**
 * The Elevation chart's button for switching views. It shows the view a tap switches to: while the chart is the single
 * line ([on] false) it shows one line splitting into three arrows (the Elevation Bands, white on dark green); while the
 * bands are on it shows three lines merging into one arrow (the single line, purple on white).
 */
@Composable
fun LayersToggleButton(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val background by animateColorAsState(if (on) Color.White.copy(alpha = 0.92f) else BandsGreen, label = "layersButtonColor")
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
        Crossfade(targetState = on, label = "layersIcon") { bandsOn ->
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 7.dp)) {
                val stroke = 2.2.dp.toPx()
                val w = size.width
                val h = size.height
                val head = minOf(w, h) * 0.2f
                val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

                // A line from [from] to [to], with an arrowhead at [to] when [arrow]
                fun line(path: Path, from: Offset, to: Offset, arrow: Boolean) {
                    path.moveTo(from.x, from.y)
                    path.lineTo(to.x, to.y)
                    if (!arrow) return
                    val angle = atan2(to.y - from.y, to.x - from.x)
                    for (side in listOf(-1f, 1f)) {
                        val back = angle + PI.toFloat() + side * ARROW_SPREAD
                        path.moveTo(to.x, to.y)
                        path.lineTo(to.x + head * cos(back), to.y + head * sin(back))
                    }
                }

                val path = Path()
                if (bandsOn) {
                    // Three tails merge into one arrow pointing up: the single line
                    val join = Offset(w * 0.5f, h * 0.5f)
                    line(path, Offset(w * 0.1f, h * 0.95f), join, arrow = false)
                    line(path, Offset(w * 0.5f, h), join, arrow = false)
                    line(path, Offset(w * 0.9f, h * 0.95f), join, arrow = false)
                    line(path, join, Offset(w * 0.5f, h * 0.02f), arrow = true)
                    drawPath(path, LayerColors[0], style = style)
                } else {
                    // One stem splits into three arrows: the Elevation Bands
                    val fork = Offset(w * 0.5f, h * 0.62f)
                    line(path, Offset(w * 0.5f, h), fork, arrow = false)
                    line(path, fork, Offset(w * 0.04f, h * 0.3f), arrow = true)
                    line(path, fork, Offset(w * 0.5f, h * 0.02f), arrow = true)
                    line(path, fork, Offset(w * 0.96f, h * 0.3f), arrow = true)
                    drawPath(path, Color.White, style = style)
                }
            }
        }
    }
}

// How far each side of an arrowhead leans from the shaft, in radians (about 35 degrees)
private const val ARROW_SPREAD = 0.61f

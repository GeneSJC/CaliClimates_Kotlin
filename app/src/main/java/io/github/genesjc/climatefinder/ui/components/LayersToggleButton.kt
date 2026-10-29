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
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** The line colour of each layer, nearest (front) first: the purple of the single line, then teal, then blue. */
val LayerColors = listOf(Color(0xFF7C3AED), Color(0xFF0D9488), Color(0xFF2563EB))

/** The dark green of the Elevation Bands controls and its notice. */
val BandsGreen = Color(0xFF14532D)

/**
 * The Elevation chart's button for switching views, white on dark green. It shows the view a tap switches to: while the
 * chart is the single line ([on] false) it shows three parallel arrows, up and to the right (the Elevation Bands);
 * while the bands are on it shows one bold arrow the same way (the single line).
 */
@Composable
fun LayersToggleButton(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = BandsGreen,
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
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp)) {
                val w = size.width
                val h = size.height
                val middle = Offset(w / 2f, h / 2f)
                // Up and to the right: along (1, -1), and across it (1, 1)
                val along = Offset(sqrt(0.5f), -sqrt(0.5f))
                val across = Offset(sqrt(0.5f), sqrt(0.5f))

                // An arrow from [from] to [to] in white: [strokeDp] thick, its head [headShare] of the icon's size
                fun arrow(from: Offset, to: Offset, strokeDp: Float, headShare: Float) {
                    val head = minOf(w, h) * headShare
                    val path = Path()
                    path.moveTo(from.x, from.y)
                    path.lineTo(to.x, to.y)
                    val angle = atan2(to.y - from.y, to.x - from.x)
                    for (side in listOf(-1f, 1f)) {
                        val back = angle + PI.toFloat() + side * ARROW_SPREAD
                        path.moveTo(to.x, to.y)
                        path.lineTo(to.x + head * cos(back), to.y + head * sin(back))
                    }
                    drawPath(
                        path, Color.White,
                        style = Stroke(width = strokeDp.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                if (bandsOn) {
                    // The single line: one bold arrow
                    val half = h * 0.468f
                    arrow(middle - along * half, middle + along * half, strokeDp = 12f, headShare = 0.3f)
                } else {
                    // The Elevation Bands: three parallel arrows side by side (the middle one a little longer)
                    val side = hypot(w * 0.5f, h * 0.32f) * 1.05f
                    val centre = h * 0.735f
                    val gap = w * 0.31f
                    listOf(side, centre, side).forEachIndexed { i, length ->
                        val c = middle + across * ((i - 1) * gap)
                        arrow(c - along * (length / 2f), c + along * (length / 2f), strokeDp = 6f, headShare = 0.2f)
                    }
                }
            }
        }
    }
}

// How far each side of an arrowhead leans from the shaft, in radians (about 35 degrees)
private const val ARROW_SPREAD = 0.61f

package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R

/** The compass's width and height (dp), which the chart also keeps its labels clear of. */
const val BAND_COMPASS_SIZE_DP = 58f

private val NEEDLE_RED = Color(0xFFEF4444)

/**
 * A small compass that shows where the chart is looking, heading-up like a boat's compass card: the way the view points is always
 * the top, and the card turns under a fixed red arrow, so looking north has N at the top and looking west has W at the top with N
 * at three o'clock. [angle] is the viewing angle in degrees from north toward west. It is only a picture of the heading (the
 * control that turns the view is separate) and takes no touches.
 */
@Composable
fun BandCompass(angle: Int, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val letters = listOf(
        0f to stringResource(R.string.elevation_heading_n),
        90f to stringResource(R.string.elevation_heading_e),
        180f to stringResource(R.string.elevation_heading_s),
        270f to stringResource(R.string.elevation_heading_w)
    )
    Surface(
        shape = CircleShape,
        color = Color(0xE61E293B),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 2.dp,
        modifier = modifier.size(BAND_COMPASS_SIZE_DP.dp)
    ) {
        Canvas(modifier = Modifier.size(BAND_COMPASS_SIZE_DP.dp).clearAndSetSemantics { }) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            // The card turns with the view: the point the view looks toward comes to the top
            rotate(degrees = angle.toFloat(), pivot = centre) {
                for (k in 0 until 24) {
                    if (k % 6 == 0) continue // the letters stand there
                    rotate(degrees = k * 15f, pivot = centre) {
                        drawLine(
                            Color.White.copy(alpha = if (k % 3 == 0) 0.5f else 0.28f),
                            Offset(centre.x, centre.y - radius + 3.dp.toPx()),
                            Offset(centre.x, centre.y - radius + (if (k % 3 == 0) 8 else 5).dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                letters.forEach { (heading, name) ->
                    val layout = measurer.measure(
                        name,
                        TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = (if (heading == 0f) Color(0xFFF87171) else Color.White).copy(alpha = if (heading == 0f || heading == 270f) 0.95f else 0.55f)
                        )
                    )
                    // Each letter stands on the card's rim, turning with it
                    rotate(degrees = heading, pivot = centre) {
                        drawText(layout, topLeft = Offset(centre.x - layout.size.width / 2f, centre.y - radius + 4.dp.toPx()))
                    }
                }
            }
            // The fixed arrow: where the view points, always up
            val arrow = Path().apply {
                moveTo(centre.x, centre.y - radius * 0.5f)
                lineTo(centre.x + 5.dp.toPx(), centre.y + 6.dp.toPx())
                lineTo(centre.x, centre.y + 2.dp.toPx())
                lineTo(centre.x - 5.dp.toPx(), centre.y + 6.dp.toPx())
                close()
            }
            drawPath(arrow, NEEDLE_RED)
        }
    }
}

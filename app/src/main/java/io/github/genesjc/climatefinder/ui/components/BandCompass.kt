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
 * A small compass that shows where the chart is looking: north at the top, a needle pointing the way the view points, swinging
 * from north toward west as [angle] (degrees from north toward west) grows. It is only a picture of the heading (the control
 * that turns the view is separate) and takes no touches.
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
            // The four points, fixed: north at the top in red
            letters.forEach { (heading, name) ->
                val layout = measurer.measure(
                    name,
                    TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = (if (heading == 0f) Color(0xFFF87171) else Color.White).copy(alpha = if (heading == 0f || heading == 270f) 0.95f else 0.5f)
                    )
                )
                rotate(degrees = 0f, pivot = centre) {
                    val rad = Math.toRadians(heading.toDouble())
                    val at = Offset(centre.x + Math.sin(rad).toFloat() * (radius - 8.dp.toPx()), centre.y - Math.cos(rad).toFloat() * (radius - 8.dp.toPx()))
                    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
                }
            }
            // The needle: red toward the heading, grey behind, swinging counter-clockwise from north as the view turns west
            rotate(degrees = -angle.toFloat(), pivot = centre) {
                val tip = radius - 15.dp.toPx()
                val head = Path().apply {
                    moveTo(centre.x, centre.y - tip)
                    lineTo(centre.x + 4.dp.toPx(), centre.y)
                    lineTo(centre.x - 4.dp.toPx(), centre.y)
                    close()
                }
                val tail = Path().apply {
                    moveTo(centre.x, centre.y + tip * 0.55f)
                    lineTo(centre.x + 4.dp.toPx(), centre.y)
                    lineTo(centre.x - 4.dp.toPx(), centre.y)
                    close()
                }
                drawPath(tail, Color.White.copy(alpha = 0.45f))
                drawPath(head, NEEDLE_RED)
            }
            drawCircle(Color.White, radius = 2.dp.toPx(), center = centre)
        }
    }
}

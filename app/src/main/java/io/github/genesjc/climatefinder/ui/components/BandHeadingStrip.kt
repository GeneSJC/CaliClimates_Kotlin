package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.BandAngle

/** The heading strip's height (dp), which the chart also keeps its labels clear of. */
const val BAND_HEADING_HEIGHT_DP = 28f

/** The gap between the strip and the slider under it (dp). */
const val BAND_HEADING_GAP_DP = 4f

/** The mark in the middle, red like the lubber line of a boat's compass, and the red of north on the card. */
private val HEADING_MARK_RED = Color(0xFFEF4444)
private val NORTH_RED = Color(0xFFF87171)

/** How many degrees of heading the strip shows across its width. */
private const val WINDOW_DEGREES = 130f

/** Where the compass names sit, as headings in degrees (west of north is negative): from north-east round to south-west. */
private val POINT_HEADINGS = listOf(45f, 0f, -22.5f, -45f, -67.5f, -90f, -135f)

/**
 * A compass strip like the heading bar of a boat's or an aircraft's instruments: the compass names scroll sideways under a fixed
 * mark in the middle as the viewing angle turns, so the name under the mark is the way the chart is looking. [angle] is the
 * viewing angle in degrees from north toward west (see [BandAngle]), so the heading is that many degrees west of north. It is
 * only a picture of the heading (the slider turns it) and takes no touches.
 */
@Composable
fun BandHeadingStrip(angle: Int, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val names = listOf(
        stringResource(R.string.elevation_heading_ne),
        stringResource(R.string.elevation_heading_n),
        stringResource(R.string.elevation_heading_nnw),
        stringResource(R.string.elevation_heading_nw),
        stringResource(R.string.elevation_heading_wnw),
        stringResource(R.string.elevation_heading_w),
        stringResource(R.string.elevation_heading_sw)
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xE61E293B),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 2.dp,
        modifier = modifier.height(BAND_HEADING_HEIGHT_DP.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val centre = size.width / 2f
            val perDegree = size.width / WINDOW_DEGREES
            val heading = -angle.toFloat()
            // A small tick every 15 degrees along the bottom edge
            val firstTick = Math.floor(((heading - WINDOW_DEGREES / 2f) / 15f).toDouble()).toInt()
            val lastTick = Math.ceil(((heading + WINDOW_DEGREES / 2f) / 15f).toDouble()).toInt()
            for (k in firstTick..lastTick) {
                val x = centre + (k * 15f - heading) * perDegree
                if (x < 8.dp.toPx() || x > size.width - 8.dp.toPx()) continue
                drawLine(Color.White.copy(alpha = 0.35f), Offset(x, size.height - 7.dp.toPx()), Offset(x, size.height - 3.dp.toPx()), strokeWidth = 1.dp.toPx())
            }
            // The names: brightest under the mark, fading toward the edges
            POINT_HEADINGS.forEachIndexed { i, pointHeading ->
                val x = centre + (pointHeading - heading) * perDegree
                val away = Math.abs(x - centre) / (size.width / 2f)
                val alpha = (1f - away).coerceIn(0f, 1f)
                if (alpha <= 0.05f) return@forEachIndexed
                val bold = away < 0.12f
                // North is red, as on a boat's compass card
                val layout = measurer.measure(
                    names[i],
                    TextStyle(fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = (if (pointHeading == 0f) NORTH_RED else Color.White).copy(alpha = 0.25f + 0.75f * alpha))
                )
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, 3.dp.toPx()))
            }
            // The mark: a short line down from the top at the middle
            drawLine(HEADING_MARK_RED, Offset(centre, 0f), Offset(centre, 5.dp.toPx()), strokeWidth = 2.5.dp.toPx())
        }
    }
}

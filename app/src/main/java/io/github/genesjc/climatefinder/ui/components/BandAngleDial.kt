package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.BandAngle
import kotlin.math.abs
import kotlin.math.atan2

/** The dial's width and height (dp), which the chart also keeps its labels clear of. */
const val BAND_DIAL_SIZE_DP = 104f

private val MARK_RED = Color(0xFFEF4444)
private val NORTH_RED_LIGHT = Color(0xFFF87171)

/** The compass names around the card, as headings in degrees (west of north is negative); the five that the dial can look at come first. */
private val CARD_HEADINGS = listOf(0f, -22.5f, -45f, -67.5f, -90f, 45f, 90f, 135f, 180f, -135f)

/**
 * The viewing angle as a round compass card, like a boat's: the card turns under a fixed red mark at the top, so the name under
 * the mark is the way the chart is looking, and the angle in degrees reads in the middle. Drag round the card to turn it; it
 * turns through 0 to 90 degrees only (north to west), snaps onto 0, 45 and 90 and ticks like the slider (see [BandAngle.snap]
 * and [BandAngle.tickBetween]). It takes the angle and reports changes and nothing else, so it can stand in for the slider.
 */
@Composable
fun BandAngleDial(angle: Int, onAngleChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val measurer = rememberTextMeasurer()
    val currentAngle = rememberUpdatedState(angle)
    val report = rememberUpdatedState(onAngleChange)
    // Where the card is under the finger, free of the detents, so a drag can leave one again
    val free = remember { mutableFloatStateOf(angle.toFloat()) }
    val names = listOf(
        stringResource(R.string.elevation_heading_n),
        stringResource(R.string.elevation_heading_nnw),
        stringResource(R.string.elevation_heading_nw),
        stringResource(R.string.elevation_heading_wnw),
        stringResource(R.string.elevation_heading_w),
        stringResource(R.string.elevation_heading_ne),
        stringResource(R.string.elevation_heading_e),
        stringResource(R.string.elevation_heading_se),
        stringResource(R.string.elevation_heading_s),
        stringResource(R.string.elevation_heading_sw)
    )
    val readingText = stringResource(R.string.elevation_dial_reading, angle)
    val description = stringResource(R.string.elevation_angle_dial)
    val state = stringResource(R.string.elevation_angle_state, angle)
    Surface(
        shape = CircleShape,
        color = Color(0xE61E293B),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 2.dp,
        modifier = modifier
            .size(BAND_DIAL_SIZE_DP.dp)
            .semantics {
                contentDescription = description
                stateDescription = state
            }
            .pointerInput(Unit) {
                var last = 0f
                detectDragGestures(
                    onDragStart = { start ->
                        free.floatValue = currentAngle.value.toFloat()
                        last = fingerAngle(start, Offset(size.width / 2f, size.height / 2f))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val now = fingerAngle(change.position, Offset(size.width / 2f, size.height / 2f))
                        // The way the finger went round the card since the last move, kept to the short way round
                        val delta = ((now - last + 540f) % 360f) - 180f
                        last = now
                        free.floatValue = (free.floatValue + delta).coerceIn(BandAngle.NORTH.toFloat(), BandAngle.WEST.toFloat())
                        val next = BandAngle.snap(free.floatValue)
                        val from = currentAngle.value
                        if (next != from) {
                            when (BandAngle.tickBetween(from, next)) {
                                BandAngle.Tick.DETENT -> AngleHaptics.detent(context)
                                BandAngle.Tick.LIGHT -> AngleHaptics.light(context)
                                BandAngle.Tick.NONE -> Unit
                            }
                            report.value(next)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(BAND_DIAL_SIZE_DP.dp)) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            val turn = angle.toFloat()
            // The card: a tick every 15 degrees and the compass names, all turning together
            rotate(degrees = turn, pivot = centre) {
                for (k in 0 until 24) {
                    val long = k % 3 == 0
                    rotate(degrees = k * 15f, pivot = centre) {
                        drawLine(
                            Color.White.copy(alpha = if (long) 0.55f else 0.3f),
                            Offset(centre.x, centre.y - radius + 3.dp.toPx()),
                            Offset(centre.x, centre.y - radius + (if (long) 9 else 6).dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                CARD_HEADINGS.forEachIndexed { i, heading ->
                    val onCard = heading + turn
                    val near = abs(((onCard + 540f) % 360f) - 180f) // degrees from the mark, 0 to 180
                    val bold = near < 12f
                    val inRange = i < 5
                    val base = if (heading == 0f) NORTH_RED_LIGHT else Color.White
                    val layout = measurer.measure(
                        names[i],
                        TextStyle(
                            fontSize = if (inRange) 12.sp else 10.sp,
                            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
                            color = base.copy(alpha = if (bold) 1f else if (inRange) 0.7f else 0.4f)
                        )
                    )
                    rotate(degrees = heading, pivot = centre) {
                        drawText(layout, topLeft = Offset(centre.x - layout.size.width / 2f, centre.y - radius + 12.dp.toPx()))
                    }
                }
            }
            // The fixed mark at the top: a small red pointer, like the lubber line of a boat's compass
            val pointer = Path().apply {
                moveTo(centre.x - 5.dp.toPx(), 0f)
                lineTo(centre.x + 5.dp.toPx(), 0f)
                lineTo(centre.x, 9.dp.toPx())
                close()
            }
            drawPath(pointer, MARK_RED)
            // The angle in the middle
            val reading = measurer.measure(
                readingText,
                TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            )
            drawText(reading, topLeft = Offset(centre.x - reading.size.width / 2f, centre.y - reading.size.height / 2f))
        }
    }
}

/** The angle of the finger round the middle of the dial, in degrees clockwise from the top. */
private fun fingerAngle(position: Offset, centre: Offset): Float {
    val dx = position.x - centre.x
    val dy = position.y - centre.y
    return Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
}

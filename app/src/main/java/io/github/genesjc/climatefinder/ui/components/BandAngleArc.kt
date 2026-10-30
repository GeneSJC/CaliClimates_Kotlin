package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** The quarter dial's width and height (dp), which the chart also keeps its labels clear of. */
const val BAND_ARC_SIZE_DP = 112f

/** How far in from the rim the track and the handle run (dp). */
private const val TRACK_INSET_DP = 24f

private const val HANDLE_RADIUS_DP = 13f

/**
 * The control that turns the viewing angle: a quarter of a dial in the bottom corner, a pie slice from north (straight up from
 * the corner) round to west (straight along the bottom edge to the left), with a round handle on a track that shows where the
 * view is. Drag the handle, or touch anywhere in the slice, to turn the view; it snaps onto 0, 45 and 90 and ticks as the
 * slider does (see [BandAngle.snap] and [BandAngle.tickBetween]). It only sets the angle: the compass that shows where the
 * view points is a separate display ([BandCompass]). It takes the angle and reports changes and nothing else, so another
 * widget can take its place.
 */
@Composable
fun BandAngleArc(angle: Int, onAngleChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val measurer = rememberTextMeasurer()
    val currentAngle = rememberUpdatedState(angle)
    val report = rememberUpdatedState(onAngleChange)
    val description = stringResource(R.string.elevation_angle_arc)
    val state = stringResource(R.string.elevation_angle_state, angle)
    val north = stringResource(R.string.elevation_heading_n)
    val west = stringResource(R.string.elevation_heading_w)
    fun set(position: Offset, pivot: Offset) {
        // The finger's angle from north (straight up from the corner) toward west (along the bottom edge to the left)
        val degrees = Math.toDegrees(atan2((pivot.x - position.x).toDouble(), (pivot.y - position.y).toDouble())).toFloat()
        val next = BandAngle.snap(degrees)
        val from = currentAngle.value
        if (next == from) return
        when (BandAngle.tickBetween(from, next)) {
            BandAngle.Tick.DETENT -> AngleHaptics.detent(context)
            BandAngle.Tick.LIGHT -> AngleHaptics.light(context)
            BandAngle.Tick.NONE -> Unit
        }
        report.value(next)
    }
    Canvas(
        modifier = modifier
            .size(BAND_ARC_SIZE_DP.dp)
            .semantics {
                contentDescription = description
                stateDescription = state
            }
            .pointerInput(Unit) {
                val pivot = Offset(size.width.toFloat(), size.height.toFloat())
                detectDragGestures(
                    onDragStart = { set(it, pivot) },
                    onDrag = { change, _ ->
                        change.consume()
                        set(change.position, pivot)
                    }
                )
            }
            .pointerInput(Unit) {
                val pivot = Offset(size.width.toFloat(), size.height.toFloat())
                detectTapGestures { set(it, pivot) }
            }
    ) {
        val pivot = Offset(size.width, size.height)
        val rim = size.width - 2.dp.toPx()
        val track = rim - TRACK_INSET_DP.dp.toPx()
        // The slice itself, dark and a little see-through like the key under it
        val slice = Path().apply {
            moveTo(pivot.x, pivot.y)
            lineTo(pivot.x, pivot.y - rim)
            arcTo(Rect(center = pivot, radius = rim), startAngleDegrees = 270f, sweepAngleDegrees = -90f, forceMoveTo = false)
            close()
        }
        drawPath(slice, Color(0xE61E293B))
        drawPath(slice, Color.White.copy(alpha = 0.18f), style = Stroke(width = 1.dp.toPx()))
        // Ticks every 15 degrees, longer at 0, 45 and 90, just inside the rim
        for (k in 0..6) {
            val degrees = k * BandAngle.TICK_EVERY
            val rad = Math.toRadians(degrees.toDouble())
            val long = BandAngle.isDetent(degrees)
            val outer = rim - 3.dp.toPx()
            val inner = outer - (if (long) 9 else 5).dp.toPx()
            drawLine(
                Color.White.copy(alpha = if (long) 0.6f else 0.35f),
                Offset(pivot.x - sin(rad).toFloat() * inner, pivot.y - cos(rad).toFloat() * inner),
                Offset(pivot.x - sin(rad).toFloat() * outer, pivot.y - cos(rad).toFloat() * outer),
                strokeWidth = 1.dp.toPx()
            )
        }
        // The track from west round to north, and the part from north to the handle lit
        val trackRect = Rect(center = pivot, radius = track)
        drawArc(Color.White.copy(alpha = 0.3f), startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = trackRect.topLeft, size = trackRect.size, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
        drawArc(Color.White.copy(alpha = 0.7f), startAngle = 270f - angle, sweepAngle = angle.toFloat(), useCenter = false, topLeft = trackRect.topLeft, size = trackRect.size, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
        // The two ends, named inside the track
        val labelRadius = track - 22.dp.toPx()
        listOf(0 to north, 90 to west).forEach { (degrees, name) ->
            val rad = Math.toRadians(degrees.toDouble())
            val layout = measurer.measure(name, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f)))
            val at = Offset(pivot.x - sin(rad).toFloat() * labelRadius, pivot.y - cos(rad).toFloat() * labelRadius)
            drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
        }
        // The handle: a round knob on the track, with a ring so it reads as something to take hold of
        val rad = Math.toRadians(angle.toDouble())
        val handle = Offset(pivot.x - sin(rad).toFloat() * track, pivot.y - cos(rad).toFloat() * track)
        drawCircle(Color.Black.copy(alpha = 0.25f), radius = HANDLE_RADIUS_DP.dp.toPx() + 2.dp.toPx(), center = handle + Offset(0f, 1.dp.toPx()))
        drawCircle(Color.White, radius = HANDLE_RADIUS_DP.dp.toPx(), center = handle)
        drawCircle(Color(0xFF1E293B), radius = 3.dp.toPx(), center = handle)
    }
}

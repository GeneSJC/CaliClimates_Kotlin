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
import androidx.compose.ui.graphics.Brush
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
const val BAND_ARC_SIZE_DP = 118f

/** How far in from the dial's bottom and right edges the middle of its ring (the hole) is (dp): half the band's thickness. */
const val BAND_ARC_CENTRE_INSET_DP = 25f

/** The gap between the dial and the key under it (dp). */
const val BAND_ARC_GAP_DP = 4f

/** The thickness of the dial's band (dp); the ends of the band are round, so its half-thickness is the radius of the ends. */
private const val BAND_WIDTH_DP = BAND_ARC_CENTRE_INSET_DP * 2f

private const val HANDLE_RADIUS_DP = 13f

/**
 * Which handle the dial has (Gene, 2026-11-04). Two variations, both kept:
 *  - true: a wide white PILL that lies along the band like a piece cut out of it, with a little dark margin of the band showing
 *    round it (the newer desktop-switch look, as on the view toggle in the header);
 *  - false: the round KNOB of 13dp radius on the thin track.
 * Set it to false to go back to the round knob.
 */
private const val HANDLE_AS_PILL = true

/** The pill is filled with the blue-to-green wash (the toggle's and the picker button's), drawn across the pill itself; it was white, then tangerine. */

/** How solid the pill's wash is: 0.9, a tenth see-through (it was solid and looked a little bright). */
private const val HANDLE_PILL_OPACITY = 0.9f

/** The pill's thickness across the band (the band less 8dp of margin each side) and its whole length along it (dp): the proportions that read as a clearly defined pill; the bigger tangerine one (54 by 70 and 80) looked amorphous. */
private const val HANDLE_PILL_THICKNESS_DP = BAND_WIDTH_DP - 16f
private const val HANDLE_PILL_LENGTH_DP = 52f

/**
 * The control that turns the viewing angle: a quarter of a ring in the bottom corner, a curved band from north (straight up from
 * the middle of the corner) round to west (straight along the bottom to the left), with round ends, a hole in the middle so the
 * chart shows through, and a handle (a wide pill with a pointer triangle in it, or a round knob: see [HANDLE_AS_PILL]) on a track that shows where the view is. Drag the handle, or touch the band, to turn
 * the view; it snaps onto 0, 45 and 90 and ticks as the slider does (see [BandAngle.snap] and [BandAngle.tickBetween]). It only
 * sets the angle: the compass that shows where the view points is a separate display ([BandCompass]). It takes the angle and
 * reports changes and nothing else, so another widget can take its place.
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
        // The finger's angle from north (straight up from the pivot) toward west (along the bottom to the left)
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
                // The middle of the ring sits a half-band in from the corner, so the round ends have room
                val pivot = Offset(size.width - BAND_WIDTH_DP.dp.toPx() / 2f, size.height - BAND_WIDTH_DP.dp.toPx() / 2f)
                val centreRadius = minOf(pivot.x, pivot.y) - BAND_WIDTH_DP.dp.toPx() / 2f - 2.dp.toPx()
                // A touch has to start on the band (a little generously), not in the hole or outside it
                fun onBand(at: Offset): Boolean {
                    val d = (at - pivot).getDistance()
                    return d in (centreRadius - BAND_WIDTH_DP.dp.toPx() / 2f - 10.dp.toPx())..(centreRadius + BAND_WIDTH_DP.dp.toPx() / 2f + 10.dp.toPx())
                }
                var dragging = false
                detectDragGestures(
                    onDragStart = {
                        dragging = onBand(it)
                        if (dragging) set(it, pivot)
                    },
                    onDrag = { change, _ ->
                        if (dragging) {
                            change.consume()
                            set(change.position, pivot)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                val pivot = Offset(size.width - BAND_WIDTH_DP.dp.toPx() / 2f, size.height - BAND_WIDTH_DP.dp.toPx() / 2f)
                val centreRadius = minOf(pivot.x, pivot.y) - BAND_WIDTH_DP.dp.toPx() / 2f - 2.dp.toPx()
                detectTapGestures {
                    val d = (it - pivot).getDistance()
                    if (d in (centreRadius - BAND_WIDTH_DP.dp.toPx() / 2f - 10.dp.toPx())..(centreRadius + BAND_WIDTH_DP.dp.toPx() / 2f + 10.dp.toPx())) set(it, pivot)
                }
            }
    ) {
        val bandWidth = BAND_WIDTH_DP.dp.toPx()
        val pivot = Offset(size.width - bandWidth / 2f, size.height - bandWidth / 2f)
        val centreRadius = minOf(pivot.x, pivot.y) - bandWidth / 2f - 2.dp.toPx()
        val outer = centreRadius + bandWidth / 2f
        val bandRect = Rect(center = pivot, radius = centreRadius)
        // The band: a quarter of a ring with round ends, dark and a little see-through like the key under it, outlined faintly
        drawArc(Color.White.copy(alpha = 0.18f), startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = bandRect.topLeft, size = bandRect.size, style = Stroke(width = bandWidth + 2.dp.toPx(), cap = StrokeCap.Round))
        drawArc(Color(0xE61E293B), startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = bandRect.topLeft, size = bandRect.size, style = Stroke(width = bandWidth, cap = StrokeCap.Round))
        // Ticks every 15 degrees, longer at 0, 45 and 90, along the outer edge
        for (k in 0..6) {
            val degrees = k * BandAngle.TICK_EVERY
            val rad = Math.toRadians(degrees.toDouble())
            val long = BandAngle.isDetent(degrees)
            val tickOuter = outer - 4.dp.toPx()
            val tickInner = tickOuter - (if (long) 9 else 5).dp.toPx()
            drawLine(
                Color.White.copy(alpha = if (long) 0.6f else 0.35f),
                Offset(pivot.x - sin(rad).toFloat() * tickInner, pivot.y - cos(rad).toFloat() * tickInner),
                Offset(pivot.x - sin(rad).toFloat() * tickOuter, pivot.y - cos(rad).toFloat() * tickOuter),
                strokeWidth = 1.dp.toPx()
            )
        }
        // The track from west round to north, and the part from north to the handle lit
        val track = centreRadius + 2.dp.toPx()
        val trackRect = Rect(center = pivot, radius = track)
        drawArc(Color.White.copy(alpha = 0.3f), startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = trackRect.topLeft, size = trackRect.size, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
        drawArc(Color.White.copy(alpha = 0.7f), startAngle = 270f - angle, sweepAngle = angle.toFloat(), useCenter = false, topLeft = trackRect.topLeft, size = trackRect.size, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
        // The two ends, named on the inner part of the band
        val labelRadius = centreRadius - 16.dp.toPx()
        listOf(0 to north, 90 to west).forEach { (degrees, name) ->
            val rad = Math.toRadians(degrees.toDouble())
            val layout = measurer.measure(name, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f)))
            val at = Offset(pivot.x - sin(rad).toFloat() * labelRadius, pivot.y - cos(rad).toFloat() * labelRadius)
            drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
        }
        if (HANDLE_AS_PILL) {
            // The handle: a wide pill along the band, round at both ends, as if cut from the band, in the blue-to-green wash
            val thickness = HANDLE_PILL_THICKNESS_DP.dp.toPx()
            // The arc between the centres of the two round ends: the whole length less the two end caps
            val sweep = Math.toDegrees(((HANDLE_PILL_LENGTH_DP.dp.toPx() - thickness) / centreRadius).toDouble()).toFloat()
            val start = 270f - angle - sweep / 2f
            drawArc(Color.Black.copy(alpha = 0.25f), startAngle = start, sweepAngle = sweep, useCenter = false, topLeft = bandRect.topLeft + Offset(0f, 1.dp.toPx()), size = bandRect.size, style = Stroke(width = thickness + 2.dp.toPx(), cap = StrokeCap.Round))
            val rad = Math.toRadians(angle.toDouble())
            val middle = Offset(pivot.x - sin(rad).toFloat() * centreRadius, pivot.y - cos(rad).toFloat() * centreRadius)
            // The wash runs diagonally across the pill (from its top-left to its bottom-right), not across the whole dial
            val reach = HANDLE_PILL_LENGTH_DP.dp.toPx() / 2f
            val wash = Brush.linearGradient(colors = BlueGreenWashColors.map { it.copy(alpha = HANDLE_PILL_OPACITY) }, start = middle - Offset(reach, reach), end = middle + Offset(reach, reach))
            drawArc(wash, startAngle = start, sweepAngle = sweep, useCenter = false, topLeft = bandRect.topLeft, size = bandRect.size, style = Stroke(width = thickness, cap = StrokeCap.Round))
            // A solid black triangle in the middle of the pill, its tip pointing the way the view looks: straight out from the
            // dial's corner, which is the heading (up at north, left at west)
            val ahead = Offset(-sin(rad).toFloat(), -cos(rad).toFloat())
            val side = Offset(-ahead.y, ahead.x)
            val tip = middle + ahead * 6.dp.toPx()
            val baseCentre = middle - ahead * 4.5.dp.toPx()
            val arrow = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(baseCentre.x + side.x * 4.5.dp.toPx(), baseCentre.y + side.y * 4.5.dp.toPx())
                lineTo(baseCentre.x - side.x * 4.5.dp.toPx(), baseCentre.y - side.y * 4.5.dp.toPx())
                close()
            }
            drawPath(arrow, Color.Black)
            return@Canvas
        }
        // The handle: a round knob on the track, with a ring so it reads as something to take hold of
        val rad = Math.toRadians(angle.toDouble())
        val handle = Offset(pivot.x - sin(rad).toFloat() * track, pivot.y - cos(rad).toFloat() * track)
        drawCircle(Color.Black.copy(alpha = 0.25f), radius = HANDLE_RADIUS_DP.dp.toPx() + 2.dp.toPx(), center = handle + Offset(0f, 1.dp.toPx()))
        drawCircle(Color.White, radius = HANDLE_RADIUS_DP.dp.toPx(), center = handle)
        drawCircle(Color(0xFF1E293B), radius = 3.dp.toPx(), center = handle)
    }
}

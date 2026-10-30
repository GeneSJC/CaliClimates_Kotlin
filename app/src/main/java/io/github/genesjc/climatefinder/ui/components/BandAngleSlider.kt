package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.BandAngle

/** The slider's height (dp), which the chart also keeps its labels clear of. */
const val BAND_ANGLE_SLIDER_HEIGHT_DP = 36f

private const val THUMB_DP = 22f

/** The gap between the slider and the key under it (dp). */
const val BAND_ANGLE_SLIDER_GAP_DP = 4f

/**
 * The control that turns the viewing direction between north and west: a thin horizontal slider in the key's dark style,
 * north at the right end and west at the left (the back band's name in the key reads left to right the same way). It
 * reports whole degrees ([onAngleChange], only when the whole degree changes) and snaps onto 0, 45 and 90 (the thumb sits on the detent)
 * with a small haptic tick. It takes the angle and reports changes and nothing else, so another widget can take its place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandAngleSlider(angle: Int, onAngleChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val colors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White.copy(alpha = 0.55f),
        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xE61E293B),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 2.dp,
        modifier = modifier.height(BAND_ANGLE_SLIDER_HEIGHT_DP.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(stringResource(R.string.elevation_angle_end_west), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
            val description = stringResource(R.string.elevation_angle_slider)
            val state = stringResource(R.string.elevation_angle_state, angle)
            Slider(
                // Left is west (90), right is north (0)
                value = (BandAngle.WEST - angle).toFloat(),
                onValueChange = { position ->
                    val next = BandAngle.snap(BandAngle.WEST - position)
                    if (next != angle) {
                        if (BandAngle.isDetent(next)) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAngleChange(next)
                    }
                },
                valueRange = BandAngle.NORTH.toFloat()..BandAngle.WEST.toFloat(),
                colors = colors,
                thumb = { Box(Modifier.size(THUMB_DP.dp).background(Color.White, CircleShape)) },
                // One unbroken track (the default leaves a gap around the thumb)
                track = { state ->
                    SliderDefaults.Track(sliderState = state, colors = colors, thumbTrackGapSize = 0.dp, drawStopIndicator = null)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .drawBehind {
                        // A small tick at each detent: 0 at the right end, 45 in the middle, 90 at the left end
                        val inset = THUMB_DP.dp.toPx() / 2f
                        val span = size.width - 2f * inset
                        for (detent in intArrayOf(BandAngle.WEST, BandAngle.MIDDLE, BandAngle.NORTH)) {
                            val x = inset + (BandAngle.WEST - detent) / BandAngle.WEST.toFloat() * span
                            drawLine(Color.White.copy(alpha = 0.6f), Offset(x, 2.dp.toPx()), Offset(x, 6.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                        }
                    }
                    .semantics {
                        contentDescription = description
                        stateDescription = state
                    }
            )
            Text(stringResource(R.string.elevation_angle_end_north), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

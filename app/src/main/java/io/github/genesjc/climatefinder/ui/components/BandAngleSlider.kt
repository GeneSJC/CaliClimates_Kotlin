package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/** The gap between the slider and the key under it (dp). */
const val BAND_ANGLE_SLIDER_GAP_DP = 4f

/**
 * The control that turns the viewing direction between north and west: a thin horizontal slider in the key's dark style,
 * north at the right end and west at the left (the back band's name in the key reads left to right the same way). It
 * reports whole degrees ([onAngleChange], only when the whole degree changes) and snaps onto 0, 45 and 90 with a small
 * haptic tick. It takes the angle and reports changes and nothing else, so another widget can take its place.
 */
@Composable
fun BandAngleSlider(angle: Int, onAngleChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    // The finger's place on the slider, free of the detents, so a drag can leave one again
    var dragged by remember { mutableStateOf<Float?>(null) }
    val shown = dragged ?: angle.toFloat()
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
                value = BandAngle.WEST - shown,
                onValueChange = { position ->
                    val free = BandAngle.WEST - position
                    dragged = free
                    val next = BandAngle.snap(free)
                    if (next != angle) {
                        if (BandAngle.isDetent(next)) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAngleChange(next)
                    }
                },
                onValueChangeFinished = { dragged = null },
                valueRange = BandAngle.NORTH.toFloat()..BandAngle.WEST.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.35f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .semantics {
                        contentDescription = description
                        stateDescription = state
                    }
            )
            Text(stringResource(R.string.elevation_angle_end_north), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

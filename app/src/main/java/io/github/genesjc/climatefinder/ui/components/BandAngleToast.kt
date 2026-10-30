package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.BandAngle
import kotlinx.coroutines.delay

/** How long the direction stays after the angle last changed. */
private const val TOAST_LINGER_MS = 1100L

private const val TOAST_FADE_IN_MS = 160
private const val TOAST_FADE_OUT_MS = 900

/**
 * Says which way the chart is looking while the viewing angle changes ("Looking west-northwest · 62°"): it fades in quickly
 * as the angle starts to move and fades out gently once it has been still for a moment. It does not show when the chart
 * first opens, nor with the bands off, where the angle has no effect. It takes no touches.
 */
@Composable
fun BandAngleToast(angle: Int, layersOn: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(angle) {
        if (!armed) {
            armed = true // the angle the chart opens with is not a change
            return@LaunchedEffect
        }
        visible = true
        delay(TOAST_LINGER_MS)
        visible = false
    }
    val target = if (visible && layersOn) 1f else 0f
    val fade by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(if (target > 0f) TOAST_FADE_IN_MS else TOAST_FADE_OUT_MS, easing = LinearEasing),
        label = "bandAngleToast"
    )
    if (fade <= 0.001f) return
    val name = stringResource(
        when (BandAngle.point(angle)) {
            0 -> R.string.elevation_look_north
            1 -> R.string.elevation_look_north_northwest
            2 -> R.string.elevation_look_northwest
            3 -> R.string.elevation_look_west_northwest
            else -> R.string.elevation_look_west
        }
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        // Green, to stand apart from the dark controls beside it
        color = BandsGreen.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        modifier = modifier.alpha(fade)
    ) {
        Text(
            text = stringResource(R.string.elevation_look_degrees, name, angle),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

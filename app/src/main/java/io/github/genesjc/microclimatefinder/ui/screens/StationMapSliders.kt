package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import io.github.genesjc.microclimatefinder.R
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A horizontal slider with a thin rail and the zoom slider's rounded-square thumb (32 x 18 dp, white edge). */
@Composable
private fun SquareThumbSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbShape = RoundedCornerShape(4.dp)
    val thumbWidth = 32.dp
    val thumbHeight = 18.dp
    val currentOnChange by rememberUpdatedState(onValueChange)
    val ratio = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    BoxWithConstraints(modifier = modifier.height(thumbHeight), contentAlignment = Alignment.CenterStart) {
        val trackWidth = maxWidth
        val thumbWidthPx = with(density) { thumbWidth.toPx() }
        val usablePx = (with(density) { trackWidth.toPx() } - thumbWidthPx).coerceAtLeast(1f)
        fun valueAt(x: Float): Float {
            val r = ((x - thumbWidthPx / 2f) / usablePx).coerceIn(0f, 1f)
            return valueRange.start + r * (valueRange.endInclusive - valueRange.start)
        }
        // The rail, and the part up to the thumb filled in
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
        )
        Box(
            modifier = Modifier
                .width(thumbWidth / 2 + (trackWidth - thumbWidth) * ratio)
                .height(5.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
        )
        Box(
            modifier = Modifier
                .offset(x = (trackWidth - thumbWidth) * ratio)
                .size(width = thumbWidth, height = thumbHeight)
                .shadow(3.dp, thumbShape)
                .background(MaterialTheme.colorScheme.primary, thumbShape)
                .border(2.dp, Color.White, thumbShape)
        )
        // The whole strip takes taps and drags
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(usablePx) { detectTapGestures { currentOnChange(valueAt(it.x)) } }
                .pointerInput(usablePx) {
                    detectHorizontalDragGestures(
                        onDragStart = { currentOnChange(valueAt(it.x)) },
                        onHorizontalDrag = { change, _ -> change.consume(); currentOnChange(valueAt(change.position.x)) }
                    )
                }
        )
    }
}

/** Horizontal dot-size control under the map: small dot to large dot. */
@Composable
internal fun PinSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.map_dot_size),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(5.dp)
        ) {}
        // The same square-thumb style as the vertical zoom slider, on a thin rail
        SquareThumbSlider(
            value = value,
            onValueChange = { onValueChange((it * 10f).roundToInt() / 10f) },
            valueRange = 1.0f..2.5f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        ) {}
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.zoom_value, (value * 10).toInt() / 10f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

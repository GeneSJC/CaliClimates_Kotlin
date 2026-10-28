package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** How long the info sheet takes to slide in and out. */
internal const val SHEET_OPEN_MS = 420
internal const val SHEET_CLOSE_MS = 320
/** How much the page behind the sheet is dimmed (0 = not at all; the layer still catches taps). */
private const val SHEET_DIM = 0f

/** The sheet is solid at its top and fades toward its bottom, where the start screen shows through. */
private const val SHEET_OPACITY_TOP = 1f
private const val SHEET_OPACITY_BOTTOM = 0.4f

/** The sheet's fill: a pastel green in light mode, a deep green in dark mode, fading downward. */
@Composable
internal fun sheetBrush(): Brush {
    val tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1F3A2A) else Color(0xFF8CC79A)
    // Fully solid through the title row, then fading down
    return Brush.verticalGradient(
        0f to tint.copy(alpha = SHEET_OPACITY_TOP),
        0.12f to tint.copy(alpha = SHEET_OPACITY_TOP),
        1f to tint.copy(alpha = SHEET_OPACITY_BOTTOM)
    )
}

/** The cards on the sheet: fully opaque, in a lighter tone than the tint, so they stand out from it. */
@Composable
internal fun sheetCardColor(): Color = MaterialTheme.colorScheme.surface


/**
 * A panel that slides in from the right edge, over the page. The page behind it stays sharp, and
 * the panel itself fades from solid at the top to mostly see-through at the bottom. Tap outside,
 * swipe it to the right, or press Back to close.
 */
@Composable
internal fun BoxScope.InfoSideSheet(
    reveal: Animatable<Float, AnimationVector1D>,
    topPx: Float,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val visible by remember { derivedStateOf { reveal.value > 0f } }
    // Wait until the anchor is measured (it is not on the first frame after coming back to Home)
    if (!visible || topPx <= 0f) return
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Invisible layer over the page; a tap on it closes the sheet
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer { alpha = reveal.value }
            .background(Color.Black.copy(alpha = SHEET_DIM))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(R.string.region_info_close)
            ) { onClose() }
    )
    var originY by remember { mutableStateOf(0f) }
    BoxWithConstraints(
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned { originY = it.positionInRoot().y }
    ) {
        // The sheet's top edge lines up with the top of the cards in the start screen's first row
        val topPx = (topPx - originY).coerceAtLeast(0f)
        val panelWidth = maxWidth * 0.9f
        val panelWidthPx = with(density) { panelWidth.toPx() }
        val dragState = rememberDraggableState { delta ->
            scope.launch { reveal.snapTo((reveal.value - delta / panelWidthPx).coerceIn(0f, 1f)) }
        }
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Level with the first row of cards, and only as tall as its content
                .offset { IntOffset(0, topPx.roundToInt()) }
                .width(panelWidth)
                .heightIn(max = maxHeight - with(density) { topPx.toDp() } - 20.dp)
                .graphicsLayer { translationX = (1f - reveal.value) * panelWidthPx }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        if (velocity > 800f || reveal.value < 0.6f) onClose() else onOpen()
                    }
                )
        ) {
            Box(modifier = Modifier.background(sheetBrush())) {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }
}

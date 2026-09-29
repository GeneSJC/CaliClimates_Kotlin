package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

// Zoom helpers shared by the Topographical Map and the Elevation Profile.

/** Highest zoom on the maps. */
const val MAX_MAP_ZOOM = 8f

/** Dots grow about 25% per zoom step, up to double, on top of any dot-size setting. */
fun zoomDotGrowth(zoom: Float): Float = (1f + (zoom - 1f) * 0.25f).coerceAtMost(2f)

/** How many labels the map may show at this zoom: [base] at full zoom-out, [perStep] more per zoom step. */
fun labelBudget(base: Int, zoom: Float, perStep: Float): Float = base + (zoom - 1f) * perStep

/**
 * Opacity for the label at [rank] (0 for the most important) given the budget: labels inside the
 * budget are solid, the next one eases in, and the rest are hidden.
 */
fun labelFade(budget: Float, rank: Int): Float = (budget - rank).coerceIn(0f, 1f)

/**
 * Keeps the zoomed content covering the viewport: pan can never expose an empty edge. The inset is the
 * margin around the content that stays put (content edges stop at the inset instead of the viewport edge).
 */
fun clampPan(pan: Offset, zoom: Float, width: Float, height: Float, insetX: Float = 0f, insetY: Float = 0f): Offset {
    val maxX = (zoom - 1f) * (width - 2f * insetX) / 2f
    val maxY = (zoom - 1f) * (height - 2f * insetY) / 2f
    return Offset(pan.x.coerceIn(-maxX, maxX), pan.y.coerceIn(-maxY, maxY))
}

/** The pan that keeps the map point under [anchor] where it is while zoom goes from [oldZoom] to [newZoom]. */
fun panForZoom(
    pan: Offset,
    oldZoom: Float,
    newZoom: Float,
    anchor: Offset,
    width: Float,
    height: Float,
    insetX: Float = 0f,
    insetY: Float = 0f
): Offset {
    val center = Offset(width / 2f, height / 2f)
    val moved = anchor - center - (anchor - center - pan) * (newZoom / oldZoom)
    return clampPan(moved, newZoom, width, height, insetX, insetY)
}

/** How much one tap on the zoom slider's + or - changes the zoom. */
const val ZOOM_BUTTON_STEP = 1.6f

/**
 * Animates a zoom change about [anchor] (the point that stays put) so labels fade in and out along
 * the way instead of jumping. Reports each frame's zoom and pan to [onFrame]. Zoom is interpolated
 * on a log scale so equal steps feel equal.
 */
suspend fun animateZoom(
    fromZoom: Float,
    fromPan: Offset,
    toZoom: Float,
    anchor: Offset,
    width: Float,
    height: Float,
    insetX: Float = 0f,
    insetY: Float = 0f,
    onFrame: (zoom: Float, pan: Offset) -> Unit
) {
    if (toZoom == fromZoom) return
    animate(0f, 1f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) { v, _ ->
        val z = fromZoom * (toZoom / fromZoom).pow(v)
        onFrame(z, panForZoom(fromPan, fromZoom, z, anchor, width, height, insetX, insetY))
    }
}

/**
 * Floating vertical zoom slider (1x to the maximum, log scale). [onValueChange] is called
 * continuously while dragging; [onStep] is called for a tap on +, - or the rail, so the caller can
 * animate those.
 */
@Composable
fun ZoomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onStep: (Float) -> Unit = onValueChange
) {
    val density = LocalDensity.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnStep by rememberUpdatedState(onStep)

    // Widget handle: Rounded-square with width increased by 80% (32.dp width x 18.dp height)
    val thumbShape = RoundedCornerShape(4.dp)
    val thumbWidth = 32.dp
    val thumbHeight = 18.dp

    // Zoom is logarithmic so each part of the rail feels like an equal step
    fun zoomFromRatio(ratio: Float) = MAX_MAP_ZOOM.pow(ratio.coerceIn(0f, 1f))
    fun ratioFromZoom(zoom: Float) = (ln(zoom.coerceAtLeast(1f)) / ln(MAX_MAP_ZOOM)).coerceIn(0f, 1f)

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: zoom in one step
            Text(
                text = "+",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { currentOnStep((value * ZOOM_BUTTON_STEP).coerceAtMost(MAX_MAP_ZOOM)) }
            )

            Text(
                text = "ZOOM",
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Draggable Track Area
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .width(36.dp)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackHeight = maxHeight
                val normalizedRatio = ratioFromZoom(value)

                val thumbHeightPx = with(density) { thumbHeight.toPx() }
                val trackHeightPx = with(density) { trackHeight.toPx() }
                val usableHeightPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

                // Track Background
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(3.dp)
                        )
                )

                // Active Filled Track (from bottom up)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .height(trackHeight * normalizedRatio)
                        .width(5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(3.dp)
                        )
                )

                // Draggable Slider Thumb: Square with rounded corners, width increased by 80% (32.dp x 18.dp)
                val thumbOffset = (trackHeight - thumbHeight) * (1f - normalizedRatio)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = thumbOffset)
                        .size(width = thumbWidth, height = thumbHeight)
                        .shadow(3.dp, thumbShape)
                        .background(MaterialTheme.colorScheme.primary, thumbShape)
                        .border(2.dp, Color.White, thumbShape)
                )

                // Full-height Touch & Drag Area overlay covering the entire rail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(trackHeightPx) {
                            detectTapGestures { tapOffset ->
                                val relativeY = (tapOffset.y - thumbHeightPx / 2f).coerceIn(0f, usableHeightPx)
                                val ratio = 1f - (relativeY / usableHeightPx)
                                val newScale = zoomFromRatio(ratio)
                                currentOnStep((newScale * 10f).roundToInt() / 10f)
                            }
                        }
                        .pointerInput(trackHeightPx) {
                            detectVerticalDragGestures(
                                onDragStart = { startOffset ->
                                    val relativeY = (startOffset.y - thumbHeightPx / 2f).coerceIn(0f, usableHeightPx)
                                    val ratio = 1f - (relativeY / usableHeightPx)
                                    val newScale = zoomFromRatio(ratio)
                                    currentOnValueChange((newScale * 10f).roundToInt() / 10f)
                                },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    val touchY = change.position.y
                                    val relativeY = (touchY - thumbHeightPx / 2f).coerceIn(0f, usableHeightPx)
                                    val ratio = 1f - (relativeY / usableHeightPx)
                                    val newScale = zoomFromRatio(ratio)
                                    currentOnValueChange((newScale * 10f).roundToInt() / 10f)
                                }
                            )
                        }
                )
            }

            // Bottom: zoom out one step
            Text(
                text = "\u2212",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { currentOnStep((value / ZOOM_BUTTON_STEP).coerceAtLeast(1f)) }
            )

            Text(
                text = "${(value * 10).toInt() / 10f}x",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

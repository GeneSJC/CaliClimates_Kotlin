package io.github.genesjc.climatefinder.ui.components

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import kotlinx.coroutines.flow.collectLatest
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
 * Keeps the zoomed content covering the viewport: pan can never expose an empty edge. The inset is the
 * margin around the content that stays put (content edges stop at the inset instead of the viewport edge).
 */
fun clampPan(
    pan: Offset,
    zoom: Float,
    width: Float,
    height: Float,
    insetX: Float = 0f,
    insetY: Float = 0f,
    overscroll: Float = 0f,
    lowestPanY: ((zoom: Float) -> Float)? = null
): Offset {
    // overscroll: how far past the edge the content may be dragged, as a fraction of the viewport,
    // at every zoom (so edge content can be pulled into clear view even when fully zoomed out)
    // lowestPanY: a further floor for the vertical pan at a zoom (the content can only be dragged up so far)
    val maxX = (zoom - 1f) * (width - 2f * insetX) / 2f + overscroll * width
    val maxY = (zoom - 1f) * (height - 2f * insetY) / 2f + overscroll * height
    val y = pan.y.coerceIn(-maxY, maxY)
    val floor = lowestPanY?.invoke(zoom)
    return Offset(pan.x.coerceIn(-maxX, maxX), if (floor == null) y else maxOf(y, minOf(floor, maxY)))
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
    insetY: Float = 0f,
    overscroll: Float = 0f,
    lowestPanY: ((zoom: Float) -> Float)? = null
): Offset {
    val center = Offset(width / 2f, height / 2f)
    val moved = anchor - center - (anchor - center - pan) * (newZoom / oldZoom)
    return clampPan(moved, newZoom, width, height, insetX, insetY, overscroll, lowestPanY)
}

/**
 * Per-label fade for the maps. [targets] says which labels should be showing (1) or hidden (0) right
 * now, keyed by label id; the returned map holds each label's current opacity, which eases toward
 * its target over [fadeSeconds] on its own clock. So a label fades in or out smoothly however the
 * zoom changed (a jump, a pinch, a pan), and the animation stops when nothing is changing.
 * A label absent from the returned map is fully hidden.
 */
@Composable
fun rememberLabelFades(targets: Map<String, Float>, fadeSeconds: Float = 0.18f): Map<String, Float> {
    val alphas = remember { mutableStateMapOf<String, Float>() }
    var latest by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    SideEffect { if (latest != targets) latest = targets.toMap() }
    // Read on every frame, so a change of speed (a slow fade while a slider moves) applies to a fade already running
    val seconds by rememberUpdatedState(fadeSeconds)
    LaunchedEffect(Unit) {
        snapshotFlow { latest }.collectLatest { wanted ->
            var lastFrame = 0L
            while (true) {
                val pending = (alphas.keys + wanted.keys).filter { id -> (alphas[id] ?: 0f) != (wanted[id] ?: 0f) }
                if (pending.isEmpty()) break
                val now = withFrameNanos { it }
                val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrame = now
                val step = dt / seconds
                pending.forEach { id ->
                    val current = alphas[id] ?: 0f
                    val goal = wanted[id] ?: 0f
                    val next = if (current < goal) minOf(current + step, goal) else maxOf(current - step, goal)
                    if (next <= 0.001f && goal == 0f) alphas.remove(id) else alphas[id] = next
                }
            }
        }
    }
    return alphas
}

/** How much one tap on the zoom slider's + or - changes the zoom. */
const val ZOOM_BUTTON_STEP = 1.6f

/** How long the ease takes when the map or chart moves to bring the selected station into view (half the speed it had at 350 ms). */
const val BRING_INTO_VIEW_MS = 700

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
    overscroll: Float = 0f,
    lowestPanY: ((zoom: Float) -> Float)? = null,
    onFrame: (zoom: Float, pan: Offset) -> Unit
) {
    if (toZoom == fromZoom) return
    animate(0f, 1f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) { v, _ ->
        val z = fromZoom * (toZoom / fromZoom).pow(v)
        onFrame(z, panForZoom(fromPan, fromZoom, z, anchor, width, height, insetX, insetY, overscroll, lowestPanY))
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
    onStep: (Float) -> Unit = onValueChange,
    /** The range and caption; the defaults make it the zoom slider. */
    minValue: Float = 1f,
    maxValue: Float = MAX_MAP_ZOOM,
    caption: String? = null
) {
    val density = LocalDensity.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnStep by rememberUpdatedState(onStep)

    // Widget handle: Rounded-square with width increased by 80% (32.dp width x 18.dp height)
    val thumbShape = RoundedCornerShape(4.dp)
    val thumbWidth = 32.dp
    val thumbHeight = 18.dp

    // Zoom is logarithmic so each part of the rail feels like an equal step
    fun zoomFromRatio(ratio: Float) = minValue * (maxValue / minValue).pow(ratio.coerceIn(0f, 1f))
    fun ratioFromZoom(zoom: Float) = (ln(zoom.coerceAtLeast(minValue) / minValue) / ln(maxValue / minValue)).coerceIn(0f, 1f)

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        // The map's pan and pinch must not act on touches that land on the slider
        modifier = modifier.touchShield()
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
                modifier = Modifier.clickable { currentOnStep((value * ZOOM_BUTTON_STEP).coerceAtMost(maxValue)) }
            )

            Text(
                text = caption ?: stringResource(R.string.zoom_caption),
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
                // The handlers below read this as it is now, so they never restart (and miss touches) while the layout settles
                val latestUsableHeightPx by rememberUpdatedState(usableHeightPx)

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
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val relativeY = (tapOffset.y - thumbHeightPx / 2f).coerceIn(0f, latestUsableHeightPx)
                                val ratio = 1f - (relativeY / latestUsableHeightPx)
                                val newScale = zoomFromRatio(ratio)
                                currentOnStep((newScale * 10f).roundToInt() / 10f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { startOffset ->
                                    val relativeY = (startOffset.y - thumbHeightPx / 2f).coerceIn(0f, latestUsableHeightPx)
                                    val ratio = 1f - (relativeY / latestUsableHeightPx)
                                    val newScale = zoomFromRatio(ratio)
                                    currentOnValueChange((newScale * 10f).roundToInt() / 10f)
                                },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    val touchY = change.position.y
                                    val relativeY = (touchY - thumbHeightPx / 2f).coerceIn(0f, latestUsableHeightPx)
                                    val ratio = 1f - (relativeY / latestUsableHeightPx)
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
                modifier = Modifier.clickable { currentOnStep((value / ZOOM_BUTTON_STEP).coerceAtLeast(minValue)) }
            )

            Text(
                text = stringResource(R.string.zoom_value, (value * 10).toInt() / 10f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

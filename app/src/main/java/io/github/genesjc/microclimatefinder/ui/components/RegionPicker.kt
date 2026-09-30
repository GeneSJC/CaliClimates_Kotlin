package io.github.genesjc.microclimatefinder.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.genesjc.microclimatefinder.ui.screens.CaliforniaRegionPickerPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * State of the region picker overlay: whether it is showing, how far its open animation is, the
 * pending (not yet confirmed) region, and where the California button sits so the panel can grow
 * out of it.
 */
@Stable
class RegionPickerState(private val scope: CoroutineScope) {
    val expand = Animatable(0f)
    var visible by mutableStateOf(false)
        private set
    var pendingRegionId by mutableStateOf("")
    var buttonBounds by mutableStateOf(Rect.Zero)

    fun open(currentRegionId: String) {
        pendingRegionId = currentRegionId
        visible = true
        scope.launch { expand.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing)) }
    }

    fun close() {
        scope.launch {
            expand.animateTo(0f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
            visible = false
        }
    }
}

@Composable
fun rememberRegionPickerState(): RegionPickerState {
    val scope = rememberCoroutineScope()
    return remember { RegionPickerState(scope) }
}

/**
 * The California button that opens the region picker. Colors default to a chip that suits a plain
 * screen header; Home passes tinted ones for its colored header card.
 */
@Composable
fun RegionPickerButton(
    state: RegionPickerState,
    selectedRegionId: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    border: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .size(44.dp)
            .clickable(
                enabled = !state.visible,
                onClickLabel = "Change region",
                role = Role.Button
            ) { state.open(selectedRegionId) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            CaliforniaOutlineIcon(
                fill = content.copy(alpha = 0.18f),
                stroke = content,
                modifier = Modifier
                    .size(28.dp)
                    .onGloballyPositioned { state.buttonBounds = Rect(it.positionInRoot(), it.size.toSize()) }
                    .alpha(if (state.visible) 0f else 1f)
            )
        }
    }
}

/**
 * The region picker overlay. Place it last inside a full-screen Box so it covers the screen: the
 * California icon grows into a panel (about 75% of the height, with a 40% scrim) holding the region
 * map and a Confirm button. Picking on the map only marks a pending region; the region changes when
 * Confirm is tapped. Back, the scrim and the close button cancel.
 */
@Composable
fun RegionPickerOverlay(
    state: RegionPickerState,
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit
) {
    BackHandler(enabled = state.visible) { state.close() }
    if (!state.visible) return

    val t = state.expand.value
    val density = LocalDensity.current
    val noRipple = remember { MutableInteractionSource() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val containerW = size.width.toFloat()
    val containerH = size.height.toFloat()
    val targetW = containerW * 0.92f
    val targetH = containerH * 0.75f
    val targetX = (containerW - targetW) / 2f
    val targetY = (containerH - targetH) / 2f
    val startX = state.buttonBounds.left - origin.x
    val startY = state.buttonBounds.top - origin.y

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInRoot()
                size = it.size
            }
    ) {
        // Scrim: tapping it closes the overlay without changing the region
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * t))
                .clickable(interactionSource = noRipple, indication = null) { state.close() }
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(lerp(startX, targetX, t).roundToInt(), lerp(startY, targetY, t).roundToInt()) }
                .size(
                    width = with(density) { lerp(state.buttonBounds.width, targetW, t).toDp() },
                    height = with(density) { lerp(state.buttonBounds.height, targetH, t).toDp() }
                )
                .clip(RoundedCornerShape(lerp(10f, 24f, t).dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = t))
                // Taps inside the panel must not reach the scrim
                .clickable(interactionSource = noRipple, indication = null) { },
            contentAlignment = Alignment.Center
        ) {
            CaliforniaOutlineIcon(
                fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                stroke = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(lerp(0f, 32f, t).dp)
                    .alpha((1f - t * 2f).coerceAtLeast(0f))
            )
            if (t > 0.5f) {
                CaliforniaRegionPickerPanel(
                    pendingRegionId = state.pendingRegionId,
                    onPickRegion = { state.pendingRegionId = it },
                    onConfirm = {
                        if (state.pendingRegionId != selectedRegionId) onSelectRegion(state.pendingRegionId)
                        state.close()
                    },
                    onClose = { state.close() },
                    modifier = Modifier
                        .requiredSize(
                            width = with(density) { targetW.toDp() },
                            height = with(density) { targetH.toDp() }
                        )
                        .alpha(((t - 0.5f) / 0.5f).coerceIn(0f, 1f))
                )
            }
        }
    }
}

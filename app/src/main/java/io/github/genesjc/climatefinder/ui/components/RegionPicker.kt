package io.github.genesjc.climatefinder.ui.components

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.ui.screens.PICKER_HEADER_HEIGHT_DP
import io.github.genesjc.climatefinder.ui.screens.PICKER_VERTICAL_STRETCH
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.border
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.climatefinder.ui.regionpicker.RegionPickerViewModel
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.genesjc.climatefinder.ui.screens.StateUsaRegionPickerPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * State of the region picker overlay: whether it is showing, how far its open animation is, the
 * pending (not yet confirmed) region, and where the state button sits so the panel can grow
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
 * The state button that opens the region picker. Colors default to a chip that suits a plain
 * screen header; Home passes tinted ones for its colored header card.
 */
@Composable
fun RegionPickerButton(
    state: RegionPickerState,
    selectedRegionId: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    border: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconSize: androidx.compose.ui.unit.Dp = 31.dp
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .size(size)
            .clickable(
                enabled = !state.visible,
                onClickLabel = stringResource(R.string.region_picker_change),
                role = Role.Button
            ) { state.open(selectedRegionId) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // The state's silhouette, the current region at full strength
            StateUsaOutlineIcon(
                fill = content.copy(alpha = 0.18f),
                stroke = content,
                regionColors = regionPickerColors(),
                selectedRegionId = selectedRegionId,
                modifier = Modifier
                    .size(iconSize)
                    .onGloballyPositioned { state.buttonBounds = Rect(it.positionInRoot(), it.size.toSize()) }
                    .alpha(if (state.visible) 0f else 1f)
            )
        }
    }
}

/** The regions' colours, the same ones the picker's map uses. */
@Composable
internal fun regionPickerColors(): Map<String, Color> {
    val stateUsa = LocalStateUsa.current
    return remember(stateUsa) {
        stateUsa.regions.associate { it.id to Color(android.graphics.Color.parseColor(it.colorHex)) }
    }
}

/**
 * The region picker overlay. Place it last inside a full-screen Box so it covers the screen: the
 * state icon grows into a panel (about 75% of the height, with a 40% scrim) holding the region
 * map. Tapping a region on the map picks it and closes the panel. Back, the scrim and the close button
 * cancel.
 */
@Composable
fun RegionPickerOverlay(
    state: RegionPickerState,
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit,
    // Created when Home first shows, so the statewide terrain picture is loaded before the picker is first opened
    viewModel: RegionPickerViewModel = viewModel()
) {
    BackHandler(enabled = state.visible) { state.close() }
    if (!state.visible) return
    val stateTerrain by viewModel.stateTerrain.collectAsStateWithLifecycle()

    val t = state.expand.value
    val density = LocalDensity.current
    val noRipple = remember { MutableInteractionSource() }
    val lightLook = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val panelTint = if (lightLook) Color(0xFFF7EFDD) else MaterialTheme.colorScheme.surface
    val panelOutline = if (lightLook) Color(0xFFE0D1B0) else Color.White.copy(alpha = 0.25f)
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val containerW = size.width.toFloat()
    val containerH = size.height.toFloat()
    val targetW = containerW * 0.92f
    // As tall as the picker's content needs (the map is 1.35 times as tall as it is wide), with a little
    // room under the map, and never more than 82% of the screen
    val contentDp = 16f + PICKER_HEADER_HEIGHT_DP + 8f + (targetW / density.density - 32f) * 1.35f * PICKER_VERTICAL_STRETCH + 16f + 14f
    val targetH = minOf(containerH * 0.82f, contentDp * density.density)
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
                // A soft shadow lifts the panel off the page, then the clip, a solid warm surface and a fine edge
                .shadow(14.dp * t, RoundedCornerShape(lerp(10f, 24f, t).dp), clip = false)
                .clip(RoundedCornerShape(lerp(10f, 24f, t).dp))
                .background(panelTint.copy(alpha = t))
                .border(1.dp, panelOutline.copy(alpha = panelOutline.alpha * t), RoundedCornerShape(lerp(10f, 24f, t).dp))
                // Taps inside the panel must not reach the scrim
                .clickable(interactionSource = noRipple, indication = null) { },
            contentAlignment = Alignment.Center
        ) {
            // Starts as the button's own icon (the state's silhouette), then fades out as the map takes over
            StateUsaOutlineIcon(
                fill = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                stroke = MaterialTheme.colorScheme.onPrimaryContainer,
                regionColors = regionPickerColors(),
                selectedRegionId = selectedRegionId,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(lerp(0f, 32f, t).dp)
                    .alpha((1f - t * 2f).coerceAtLeast(0f))
            )
            if (t > 0.5f) {
                StateUsaRegionPickerPanel(
                    pendingRegionId = state.pendingRegionId,
                    terrain = stateTerrain,
                    // A pick is the choice: no Confirm button
                    onPickRegion = { id ->
                        state.pendingRegionId = id
                        if (id != selectedRegionId) onSelectRegion(id)
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

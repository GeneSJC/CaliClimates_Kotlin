package io.github.genesjc.microclimatefinder.ui.components

import io.github.genesjc.microclimatefinder.ui.screens.PICKER_VERTICAL_STRETCH
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.border
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.microclimatefinder.ui.regionpicker.RegionPickerViewModel
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
                onClickLabel = "Change region",
                role = Role.Button
            ) { state.open(selectedRegionId) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Only the selected region's shape, in its picker colour, so it fills the button
            RegionShapeIcon(
                regionId = selectedRegionId,
                fill = regionPickerColors()[selectedRegionId] ?: content.copy(alpha = 0.18f),
                stroke = content,
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
internal fun regionPickerColors(): Map<String, Color> = remember {
    CaliforniaRegionalDefinitions.ALL_REGIONS.associate { it.id to Color(android.graphics.Color.parseColor(it.colorHex)) }
}

/**
 * The region picker overlay. Place it last inside a full-screen Box so it covers the screen: the
 * California icon grows into a panel (about 75% of the height, with a 40% scrim) holding the region
 * map. Tapping a region on the map picks it and closes the panel. Back, the scrim and the close button
 * cancel.
 */
@Composable
fun RegionPickerOverlay(
    state: RegionPickerState,
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit,
    // Created when Home first shows, so the statewide terrain picture is loaded before the picker is first opened
    viewModel: RegionPickerViewModel = viewModel(factory = RegionPickerViewModel.factory(LocalContext.current))
) {
    BackHandler(enabled = state.visible) { state.close() }
    if (!state.visible) return
    val stateTerrain by viewModel.stateTerrain.collectAsStateWithLifecycle()

    val t = state.expand.value
    val density = LocalDensity.current
    val noRipple = remember { MutableInteractionSource() }
    val lightLook = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val panelTint = if (lightLook) Color(0xFFF1E2C4) else MaterialTheme.colorScheme.surface
    val panelOutline = if (lightLook) Color(0xFF3A2A1E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.25f)
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val containerW = size.width.toFloat()
    val containerH = size.height.toFloat()
    val targetW = containerW * 0.92f
    // As tall as the picker's content needs (the map is 1.35 times as tall as it is wide), with a little
    // room under the map, and never more than 82% of the screen
    val contentDp = 16f + 32f + 12f + (targetW / density.density - 32f) * 1.35f * PICKER_VERTICAL_STRETCH + 16f + 14f
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
                .clip(RoundedCornerShape(lerp(10f, 24f, t).dp))
                .background(
                    // Solid through the title row, then fading to half see-through toward the bottom. In light
                    // mode a warm sand, to sit with the map's parchment and pills; in dark mode the surface colour
                    Brush.verticalGradient(
                        0f to panelTint.copy(alpha = t),
                        0.14f to panelTint.copy(alpha = t),
                        1f to panelTint.copy(alpha = 0.5f * t)
                    )
                )
                // A fine dark outline, so the panel reads against whatever is behind it
                .border(1.dp, panelOutline.copy(alpha = panelOutline.alpha * t), RoundedCornerShape(lerp(10f, 24f, t).dp))
                // Taps inside the panel must not reach the scrim
                .clickable(interactionSource = noRipple, indication = null) { },
            contentAlignment = Alignment.Center
        ) {
            // Starts as the button's own icon (the selected region's shape), then fades out as the map takes over
            RegionShapeIcon(
                regionId = selectedRegionId,
                fill = regionPickerColors()[selectedRegionId] ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                stroke = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(lerp(0f, 32f, t).dp)
                    .alpha((1f - t * 2f).coerceAtLeast(0f))
            )
            if (t > 0.5f) {
                CaliforniaRegionPickerPanel(
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

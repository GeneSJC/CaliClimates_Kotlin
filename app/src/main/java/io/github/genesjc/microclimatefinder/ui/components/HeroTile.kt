package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch

/** How long a tile's panel takes to grow out of it, and to fold back. */
private const val HERO_OPEN_MS = 650
private const val HERO_CLOSE_MS = 500

/**
 * A half-width tile that grows, in width and height, into a full-width [panel] floating over the
 * page, so nothing on the page moves. Tapping the panel, tapping outside, or pressing Back folds it
 * back. [fullWidthPx] is the width the panel grows to (the row the tile sits in). [tileColor] fills
 * the tile; [panelColor] is the panel's backing, which must be opaque.
 */
@Composable
fun HeroTile(
    fullWidthPx: Int,
    tileColor: Color,
    panelColor: Color,
    openLabel: String,
    closeLabel: String,
    modifier: Modifier = Modifier,
    tileBorder: BorderStroke? = null,
    resetKey: Any? = null,
    /** True: touches outside the open panel reach what is underneath (the map's dots) and do not fold it. */
    passThrough: Boolean = false,
    tile: @Composable () -> Unit,
    panel: @Composable () -> Unit
) {
    var open by remember(resetKey) { mutableStateOf(false) }
    var cardWidthPx by remember { mutableStateOf(0) }
    var cardHeightPx by remember { mutableStateOf(0) }
    val reveal = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val shape = RoundedCornerShape(16.dp)

    fun openPanel() {
        open = true
        scope.launch { reveal.animateTo(1f, tween(HERO_OPEN_MS, easing = FastOutSlowInEasing)) }
    }
    fun closePanel() {
        scope.launch {
            reveal.animateTo(0f, tween(HERO_CLOSE_MS, easing = FastOutSlowInEasing))
            open = false
        }
    }

    if (passThrough) androidx.activity.compose.BackHandler(enabled = open) { closePanel() }

    // The Box is the popup's anchor: it sits exactly on the collapsed tile
    Box(modifier = modifier.fillMaxSize()) {
        // The collapsed tile keeps its place in the layout
        Surface(
            shape = shape,
            color = tileColor,
            border = tileBorder,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    cardWidthPx = it.width
                    cardHeightPx = it.height
                }
                .clickable(onClickLabel = openLabel, role = Role.Button) { openPanel() }
        ) {
            tile()
        }

        if (open) {
            // Grows from the tile's size to the full row width, from the tile's top-left corner
            val p = reveal.value
            val startWidth = with(density) { cardWidthPx.toDp() }
            val fullWidth = with(density) { fullWidthPx.toDp() }
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { closePanel() },
                properties = PopupProperties(focusable = !passThrough, dismissOnClickOutside = !passThrough)
            ) {
                Box(
                    modifier = Modifier
                        .width(lerp(startWidth, fullWidth, p))
                        .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.8f).dp)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val h = (cardHeightPx + (placeable.height - cardHeightPx) * p).toInt()
                            layout(placeable.width, h) { placeable.place(0, 0) }
                        }
                        .shadow(12.dp * p, shape)
                        .clip(shape)
                        .background(panelColor)
                        .clickable(onClickLabel = closeLabel, role = Role.Button) { closePanel() }
                ) {
                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        // Laid out at full width and revealed as the panel grows; the tile's own
                        // content fades out as the full content fades in
                        Box(modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) {
                            Box(
                                modifier = Modifier
                                    .width(fullWidth)
                                    .graphicsLayer { alpha = ((p - 0.25f) / 0.5f).coerceIn(0f, 1f) }
                            ) {
                                panel()
                            }
                            Box(
                                modifier = Modifier
                                    .width(startWidth)
                                    .graphicsLayer { alpha = (1f - p * 3f).coerceIn(0f, 1f) }
                            ) {
                                tile()
                            }
                        }
                    }
                }
            }
        }
    }
}

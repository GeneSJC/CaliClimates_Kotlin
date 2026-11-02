package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/**
 * Three fish swimming in the ocean (decoration). They are drawn in their own layer, so only it redraws as they move, and they
 * follow the ocean when the chart is zoomed or panned. [viewY] turns a chart height into a screen height at the current zoom and pan.
 */
@Composable
internal fun ElevationFishLayer(
    zoom: Float,
    pan: Offset,
    seaMix: Float,
    viewY: (Float) -> Float,
    paddingPx: Float,
    heightPx: Float
) {
    // The effects below watch the current zoom and pan, which arrive as plain values
    val zoomNow by androidx.compose.runtime.rememberUpdatedState(zoom)
    val panNow by androidx.compose.runtime.rememberUpdatedState(pan)
    var fishTime by remember { mutableStateOf(0f) }
    var chartMoving by remember { mutableStateOf(true) }
    val fishContext = androidx.compose.ui.platform.LocalContext.current
    val animationsOn = remember {
        android.provider.Settings.Global.getFloat(
            fishContext.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) > 0f
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Each change of zoom or pan marks the chart as moving until it has been still for a little while
        androidx.compose.runtime.snapshotFlow { zoomNow to panNow }.collectLatest {
            chartMoving = true
            kotlinx.coroutines.delay(600)
            chartMoving = false
        }
    }
    androidx.compose.runtime.LaunchedEffect(animationsOn) {
        if (!animationsOn) return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow { chartMoving }.collectLatest { moving ->
            if (!moving) {
                var last = System.nanoTime()
                while (true) {
                    kotlinx.coroutines.delay(FISH_STEP_MS)
                    val now = System.nanoTime()
                    fishTime = (fishTime + (now - last) / 1e9f / FISH_LOOP_SECONDS) % 1f
                    last = now
                }
            }
        }
    }
    // The fish live in the foreground water, so they fade out with it when the sea moves to the horizon (drawn in
    // their own layer above the chart, they would swim over the land in front of a sea at the horizon)
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 1f - seaMix }) {
        val seaTop = viewY(paddingPx + (1f - SEA_FRACTION) * heightPx)
        val seaBottom = maxOf(size.height, viewY(size.height - paddingPx) + paddingPx)
        val band = seaBottom - seaTop
        if (seaTop < size.height && band > 40.dp.toPx()) {
            clipRect(top = maxOf(0f, seaTop), bottom = size.height) {
                for (fish in FISH) {
                    val length = fish.lengthDp.dp.toPx()
                    val travel = size.width + 2f * length
                    val phase = (fishTime * fish.cycles + fish.offset) % 1f
                    val cx = if (fish.rightward) phase * travel - length else size.width + length - phase * travel
                    val wobble = kotlin.math.sin((fishTime * 60f * fish.cycles + fish.offset) * 2f * Math.PI.toFloat())
                    val cy = seaTop + band * fish.depth + wobble * 4.dp.toPx()
                    drawFish(cx, cy, length, fish.color, fish.rightward, wobble)
                }
            }
        }
    }
}

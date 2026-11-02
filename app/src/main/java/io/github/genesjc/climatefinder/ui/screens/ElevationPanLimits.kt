package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset

/**
 * The lowest vertical pan the chart allows at [zoom] on a chart [canvasH] tall: any lower and the sea would take more
 * than [MAX_WATER_SHARE] of the height. Dragging the chart up stops here, which leaves a clear stopping point with the
 * water and the fish in view.
 */
internal fun elevationLowestPanY(zoom: Float, canvasH: Float): Float {
    val heightPx = canvasH - 2 * CHART_PADDING_PX
    val seaY = CHART_PADDING_PX + (1f - SEA_FRACTION) * heightPx
    return canvasH * (1f - MAX_WATER_SHARE) - canvasH / 2f - (seaY - canvasH / 2f) * zoom
}

/**
 * Eases the pan from [from] to [target] over [durationMs], handing each step to [setPan]. It stops applying the moment the person
 * touches the chart ([touchedAt] moves past the start of the move), so an automatic move never fights the finger.
 */
internal suspend fun easePanTo(from: Offset, target: Offset, durationMs: Int, touchedAt: LongArray, setPan: (Offset) -> Unit) {
    val startedAt = System.nanoTime()
    animate(0f, 1f, animationSpec = tween(durationMs, easing = FastOutSlowInEasing)) { v, _ ->
        if (touchedAt[0] < startedAt) setPan(Offset(from.x + (target.x - from.x) * v, from.y + (target.y - from.y) * v))
    }
}

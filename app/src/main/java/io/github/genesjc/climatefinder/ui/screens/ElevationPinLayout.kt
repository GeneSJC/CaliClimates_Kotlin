package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.genesjc.climatefinder.domain.BandAngle
import io.github.genesjc.climatefinder.domain.DepthBands
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation

/**
 * Where each station stands across the chart (0 at the left edge of the stations, 1 at the right) with the bands on: in its
 * order across the line of sight (west to east looking north, south to north looking west), evenly spaced in that order, or by
 * its true place on the map, or a blend of the two ([SKYLINE_EVEN_SPACING]). One entry per station, in [sorted] order.
 */
internal fun lateralPositions(sorted: List<MicroclimateStation>, angleShown: Int): FloatArray {
    if (sorted.isEmpty()) return FloatArray(0)
    val keys = sorted.map { DepthBands.lateralKey(it.latitude, it.longitude, angleShown.toDouble()) }
    val low = keys.min()
    val span = (keys.max() - low).takeIf { it > 1e-9 } ?: 1.0
    val order = keys.indices.sortedBy { keys[it] }
    val rankFrac = FloatArray(sorted.size)
    order.forEachIndexed { rank, i -> rankFrac[i] = rank.toFloat() / (sorted.size - 1).coerceAtLeast(1) }
    return FloatArray(sorted.size) {
        val trueFrac = ((keys[it] - low) / span).toFloat()
        trueFrac + (rankFrac[it] - trueFrac) * SKYLINE_EVEN_SPACING
    }
}

/**
 * A station's x on the chart (before the zoom and pan): where it ranks by height with the single line ([mix] 0), where it
 * stands across the view with the bands on ([mix] 1, from [lateralFrac]), and between while the bands fold in or out.
 * [idx] is the station's place in the height-sorted list of [count] stations.
 */
internal fun elevationPinX(idx: Int, count: Int, mix: Float, lateralFrac: FloatArray, paddingPx: Float, widthPx: Float): Float {
    val byHeight = paddingPx + idx.toFloat() / (count - 1).coerceAtLeast(1) * widthPx
    val byPlace = paddingPx + lateralFrac[idx] * widthPx
    return byHeight + (byPlace - byHeight) * mix
}

/**
 * A station's y on the chart (before the zoom and pan): its height in the plain profile with the single line ([mix] 0), and with
 * the bands on ([mix] 1) its height compressed toward the sea ([SKYLINE_HEIGHT_SCALE]) and lifted by its [band], with the lines
 * tilted as the view turns toward west ([angleShown]). [lateral] is the station's place across the view (see [lateralPositions]).
 */
internal fun elevationPinY(
    elevationMeters: Int,
    maxElevation: Float,
    band: Int,
    lateral: Float,
    mix: Float,
    angleShown: Int,
    paddingPx: Float,
    widthPx: Float,
    heightPx: Float
): Float {
    val layerTopY = paddingPx + PROFILE_HEAD * heightPx
    val layerSeaY = paddingPx + (1f - SEA_FRACTION) * heightPx
    val plain = paddingPx + profileFrac(elevationMeters / maxElevation) * heightPx
    // Turning the view toward west tilts the bands, their lines rising toward the left (the west) and the far bands tilting a
    // little less than the near ones, so the lines converge like lines in perspective: the bands seem to turn with the view
    val across = lateral - 0.5f
    val tilt = BAND_TILT * (angleShown / BandAngle.WEST.toFloat()) * (1f - BAND_TILT_CONVERGE * band) * across * widthPx
    // The bands show the heights compressed toward the sea ([SKYLINE_HEIGHT_SCALE]), so each band's swings fit on the screen
    val compressed = layerSeaY - (layerSeaY - plain) * SKYLINE_HEIGHT_SCALE
    return plain + (DepthBands.layeredY(compressed, band, layerTopY, layerSeaY) - plain) * mix + tilt * mix
}

/**
 * True while the viewing angle is moving and for a moment after ([TURNING_LINGER_MS]): the labels fade slowly then, so they
 * do not pop in and out as the stations regroup. It is false when the chart is first shown.
 */
@Composable
internal fun rememberTurning(bandAngle: Int): Boolean {
    var turning by remember { mutableStateOf(false) }
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(bandAngle) {
        if (!armed) {
            armed = true
            return@LaunchedEffect
        }
        turning = true
        kotlinx.coroutines.delay(TURNING_LINGER_MS)
        turning = false
    }
    return turning
}

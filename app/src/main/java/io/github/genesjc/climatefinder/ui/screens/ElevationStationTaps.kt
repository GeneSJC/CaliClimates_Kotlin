package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation

/** How near a tap must be to a station's dot, squared in pixels, to select it (about 42 px). */
private const val TAP_REACH_SQUARED = 1800f

/**
 * Tapping near a station's dot on the chart selects the nearest one within reach, counts as a tap on the line ([onLineTapped])
 * and calls [onHit]. [stationPosition] gives the screen position of the station at an index of [sorted]; it is read at the moment
 * of each tap, so a chart that has moved or regrouped since the gesture started still answers with where the dots are now.
 */
@Composable
internal fun Modifier.elevationStationTaps(
    sorted: List<MicroclimateStation>,
    canvasW: Float,
    stationPosition: (Int) -> Offset,
    onSelectStation: (MicroclimateStation) -> Unit,
    onLineTapped: () -> Unit,
    onHit: () -> Unit
): Modifier {
    val position by rememberUpdatedState(stationPosition)
    val select by rememberUpdatedState(onSelectStation)
    val lineTapped by rememberUpdatedState(onLineTapped)
    val hit by rememberUpdatedState(onHit)
    return pointerInput(sorted, canvasW) {
        detectTapGestures { tapOffset ->
            var closestStation: MicroclimateStation? = null
            var closestDist = Float.MAX_VALUE
            sorted.forEachIndexed { index, station ->
                val spot = position(index)
                val dx = tapOffset.x - spot.x
                val dy = tapOffset.y - spot.y
                val dist = dx * dx + dy * dy
                if (dist < closestDist && dist < TAP_REACH_SQUARED) {
                    closestDist = dist
                    closestStation = station
                }
            }
            closestStation?.let {
                select(it)
                lineTapped()
                hit()
            }
        }
    }
}

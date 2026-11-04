package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation

/**
 * What keeps the selected station in view on the Elevation chart, and what the Locate button on Home needs from it:
 *  - it reports whether the selected station is out of the chart's view ([visible] says whether a station's pin is on screen), so
 *    Home can show the button while it is, worked out afresh from the zoom and pan on every change, and reports false when the chart goes;
 *  - a tap on the button ([recenterTick] goes up) eases the chart over so the station is in the middle ([centre]);
 *  - when the selected station changes (from the Stations list, the tile or the other view) and it is not comfortably in view
 *    ([comfortable]), the chart eases over so it is.
 * The three are given as functions of the station's place in [sorted], so this holds none of the chart's geometry. [centre] is a
 * suspend function, because the chart's easing is: it runs inside the effects' coroutines.
 */
@Composable
internal fun ElevationLocateEffects(
    selectedStationId: String?,
    sorted: List<MicroclimateStation>,
    recenterTick: Int,
    onSelectedOffscreenChange: ((Boolean) -> Unit)?,
    comfortable: (Int) -> Boolean,
    visible: (Int) -> Boolean,
    centre: suspend (Int) -> Unit
) {
    val idx = sorted.indexOfFirst { it.id == selectedStationId }
    val offscreen = idx >= 0 && !visible(idx)
    LaunchedEffect(offscreen) { onSelectedOffscreenChange?.invoke(offscreen) }
    DisposableEffect(Unit) { onDispose { onSelectedOffscreenChange?.invoke(false) } }
    LaunchedEffect(recenterTick) { if (recenterTick > 0 && idx >= 0) centre(idx) }
    LaunchedEffect(selectedStationId, sorted) { if (idx >= 0 && !comfortable(idx)) centre(idx) }
}

package io.github.genesjc.climatefinder.ui.map

import androidx.lifecycle.viewModelScope
import io.github.genesjc.climatefinder.data.local.AppSettings
import io.github.genesjc.climatefinder.data.remote.ObservationResult
import io.github.genesjc.climatefinder.domain.BandAngle
import io.github.genesjc.climatefinder.domain.BandAxis
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * The Elevation view's ViewModel: what [MapViewModel] provides, plus the region's skylines, the selected
 * station's live reading and the "tap the line" hint.
 */
class ElevationViewModel : MapViewModel() {

    private val settings: AppSettings by inject()

    private val layersOn = MutableStateFlow(settings.elevationLayers())

    /** Whether the chart shows its layers (the bands) instead of the single line. */
    val layers: StateFlow<Boolean> = layersOn

    fun toggleLayers() {
        layersOn.value = !layersOn.value
        settings.saveElevationLayers(layersOn.value)
    }

    private val angle = MutableStateFlow(settings.elevationBandAngle())
    private val axis = MutableStateFlow(BandAngle.nearestAxis(angle.value))

    /** The direction the bands look across the region, in whole degrees from north (0, the default) toward west (90). */
    val bandAngle: StateFlow<Int> = angle

    /** The end of the range the angle is nearer to: north to south or west to east. */
    val bandAxis: StateFlow<BandAxis> = axis

    fun setBandAngle(degrees: Int) {
        val whole = BandAngle.clamp(degrees.toFloat())
        if (whole == angle.value) return
        angle.value = whole
        axis.value = BandAngle.nearestAxis(whole)
        settings.saveElevationBandAngle(whole)
    }


    private val skylines = HashMap<String, MutableStateFlow<List<FloatArray>?>>()

    /** The region's real skylines for the distant ranges: null until loaded, or when it has none. */
    fun skyline(regionId: String): StateFlow<List<FloatArray>?> {
        skylines[regionId]?.let { return it }
        skylines.clear()
        return MutableStateFlow<List<FloatArray>?>(null).also { flow ->
            skylines[regionId] = flow
            viewModelScope.launch { flow.value = terrains.skyline(regionId) }
        }
    }

    /** The station's latest known reading, without fetching. */
    fun knownReading(stationId: String): ObservationResult? = readings.peek(stationId)

    /** The station's live reading (fetched when not fresh); [ObservationResult.NoReading] when it cannot report one. */
    suspend fun latestReading(station: MicroclimateStation): ObservationResult =
        if (station.canHaveLiveReading) readings.get(station.id) else ObservationResult.NoReading

    /** Counts this visit to the view and says whether the hint in words should show. */
    fun startVisit(): Boolean {
        settings.countElevationHintLaunch()
        return settings.shouldShowElevationHint()
    }

    /** The user tapped the line, so the hint in words stops showing. */
    fun lineTapped() = settings.markElevationLineTapped()
}

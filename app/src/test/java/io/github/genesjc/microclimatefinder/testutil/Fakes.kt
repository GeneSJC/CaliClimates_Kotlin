package io.github.genesjc.microclimatefinder.testutil

import androidx.compose.ui.graphics.ImageBitmap
import io.github.genesjc.microclimatefinder.data.local.AppSettings
import io.github.genesjc.microclimatefinder.data.local.RegionDatasets
import io.github.genesjc.microclimatefinder.data.local.StateTerrain
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationSource
import io.github.genesjc.microclimatefinder.data.local.TerrainImage
import io.github.genesjc.microclimatefinder.data.local.TerrainSource
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.data.remote.ObservationSource
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.testStation

/** Fakes for the platform edge, shared by the ViewModel tests. Each records what a test wants to check. */

class FakeAppSettings(
    var region: String = "a",
    var stationRegion: String? = null,
    var station: String? = null,
    var zoomRegion: String? = null,
    var zoom: Float = 1f,
    var dark: Boolean = false,
    var launches: Int = 0,
    var tapped: Boolean = false
) : AppSettings {
    override fun selectedRegionId() = region
    override fun saveSelectedRegionId(regionId: String) {
        region = regionId
    }

    override fun selectedStationId(regionId: String) = station?.takeIf { stationRegion == regionId }
    override fun saveSelectedStationId(regionId: String, stationId: String?) {
        stationRegion = regionId
        station = stationId
    }

    override fun mapZoom(regionId: String) = if (zoomRegion == regionId) zoom else 1f
    override fun saveMapZoom(regionId: String, zoom: Float) {
        zoomRegion = regionId
        this.zoom = zoom
    }

    override fun darkMode() = dark
    override fun saveDarkMode(dark: Boolean) {
        this.dark = dark
    }

    override fun countElevationHintLaunch() {
        launches++
    }

    override fun shouldShowElevationHint() = !tapped && launches <= 3
    override fun markElevationLineTapped() {
        tapped = true
    }
}

/** A dataset for any region id, with one featured station named after the region. */
class FakeDatasets : RegionDatasets {
    override fun datasetFor(regionId: String) = StationDatasetInfo(
        regionId = regionId, fileName = "$regionId.txt", title = regionId.uppercase(), subtitle = "",
        stationCount = 1, minElevationMeters = 0, maxElevationMeters = 1
    )

    override fun defaultDataset() = datasetFor("default")
    override fun fullDataRegionCount() = 5
    override fun totalRegionCount() = 6
    override suspend fun stationsFor(regionId: String): List<MicroclimateStation> =
        listOf(testStation(id = regionId, isFeatured = true))
}

/** Station packs: the same stations for every dataset, or one featured station named after the dataset's region. */
class FakeStationSource(private val stations: List<MicroclimateStation>? = null) : StationSource {
    override fun loadStations(dataset: StationDatasetInfo): List<MicroclimateStation> =
        stations ?: listOf(testStation(id = dataset.regionId, isFeatured = true))
}

/** Terrain pictures: always missing (null), but records what was asked for. */
class FakeTerrainSource : TerrainSource {
    val terrainLoads = mutableListOf<String>()
    val skylineLoads = mutableListOf<String>()
    var stateLoads = 0
    var backdropLoads = 0

    override fun load(regionId: String): TerrainImage? {
        terrainLoads += regionId
        return null
    }

    override fun loadSkyline(regionId: String): List<FloatArray>? {
        skylineLoads += regionId
        return listOf(floatArrayOf(0f, 1f))
    }

    override fun loadState(): StateTerrain? {
        stateLoads++
        return null
    }

    override fun loadBackdropRelief(): ImageBitmap? {
        backdropLoads++
        return null
    }
}

/** Live observations: every fetch answers [result] and is recorded; [onFetch] can suspend a fetch. */
class FakeObservationSource(
    var result: ObservationResult = ObservationResult.Failed,
    private val onFetch: suspend (String) -> Unit = {}
) : ObservationSource {
    val fetched = mutableListOf<String>()

    override suspend fun fetchLatest(stationId: String): ObservationResult {
        fetched += stationId
        onFetch(stationId)
        return result
    }
}

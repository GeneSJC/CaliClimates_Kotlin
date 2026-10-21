package io.github.genesjc.microclimatefinder.testutil

import androidx.compose.ui.graphics.ImageBitmap
import io.github.genesjc.microclimatefinder.data.local.DarkModeStore
import io.github.genesjc.microclimatefinder.data.local.ElevationHintStore
import io.github.genesjc.microclimatefinder.data.local.MapZoomStore
import io.github.genesjc.microclimatefinder.data.local.RegionDatasets
import io.github.genesjc.microclimatefinder.data.local.SelectedRegionStore
import io.github.genesjc.microclimatefinder.data.local.SelectedStationStore
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

class FakeRegionStore(var saved: String = "a") : SelectedRegionStore {
    override fun load() = saved
    override fun save(regionId: String) {
        saved = regionId
    }
}

class FakeStationStore(var region: String? = null, var station: String? = null) : SelectedStationStore {
    override fun load(regionId: String) = station?.takeIf { region == regionId }
    override fun save(regionId: String, stationId: String?) {
        region = regionId
        station = stationId
    }
}

class FakeZoomStore(var region: String? = null, var zoom: Float = 1f) : MapZoomStore {
    override fun load(regionId: String) = if (region == regionId) zoom else 1f
    override fun save(regionId: String, zoom: Float) {
        region = regionId
        this.zoom = zoom
    }
}

class FakeDarkModeStore(var dark: Boolean = false) : DarkModeStore {
    override fun darkMode() = dark
    override fun saveDarkMode(dark: Boolean) {
        this.dark = dark
    }
}

class FakeHintStore(var launches: Int = 0, var tapped: Boolean = false) : ElevationHintStore {
    override fun countLaunch() {
        launches++
    }

    override fun shouldShow() = !tapped && launches <= 3
    override fun markLineTapped() {
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

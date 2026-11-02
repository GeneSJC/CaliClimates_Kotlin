package io.github.genesjc.climatefinder.testutil

import android.graphics.Bitmap
import io.github.genesjc.climatefinder.data.local.AppSettings
import io.github.genesjc.climatefinder.domain.model.DatasetKind
import io.github.genesjc.climatefinder.data.local.RegionDatasetCatalog
import io.github.genesjc.climatefinder.data.local.StateUsaTerrain
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo
import io.github.genesjc.climatefinder.data.local.StationSource
import io.github.genesjc.climatefinder.data.local.TerrainImage
import io.github.genesjc.climatefinder.data.local.TerrainSource
import io.github.genesjc.climatefinder.domain.model.ObservationResult
import io.github.genesjc.climatefinder.data.remote.ObservationSource
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.testStation
import io.github.genesjc.climatefinder.viewmodel.StringResolver

/** Fakes for the platform edge, shared by the ViewModel tests. Each records what a test wants to check. */

class FakeAppSettings(
    var region: String = "a",
    var stationRegion: String? = null,
    var station: String? = null,
    var zoomRegion: String? = null,
    var zoom: Float = 1f,
    var dark: Boolean = false,
    var launches: Int = 0,
    var tapped: Boolean = false,
    var layers: Boolean = true,
    var angle: Int = 0,
    var tab: Int = 0
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

    override fun elevationLayers() = layers
    override fun saveElevationLayers(on: Boolean) {
        layers = on
    }

    override fun homeTab() = tab
    override fun saveHomeTab(tab: Int) {
        this.tab = tab
    }

    override fun elevationBandAngle() = angle
    override fun saveElevationBandAngle(degrees: Int) {
        angle = degrees
    }
}

/** A dataset for any region id, with one featured station named after the region. */
class FakeDatasets : RegionDatasetCatalog {
    override suspend fun datasetFor(regionId: String) = cachedDatasetFor(regionId)

    override fun cachedDatasetFor(regionId: String) = StationDatasetInfo(
        regionId = regionId, fileName = "$regionId.txt", kind = DatasetKind.BUNDLED_PACK,
        stationCount = 1, minElevationMeters = 0, maxElevationMeters = 1
    )

    override suspend fun defaultDataset() = cachedDatasetFor("default")
    override fun emptyDefaultDataset() = cachedDatasetFor("empty")
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

    override fun loadState(): StateUsaTerrain? {
        stateLoads++
        return null
    }

    override fun loadBackdropRelief(): Bitmap? {
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

/**
 * A [StringResolver] that echoes the resource id and arguments, so a test builds the expected text with the same call
 * and checks that the code chose the right resource with the right arguments (not the English sentence).
 */
object FakeStringResolver : StringResolver {
    override fun invoke(id: Int, vararg args: Any): String = "string:$id${args.joinToString(prefix = "(", postfix = ")")}"

    override fun plural(id: Int, count: Int, vararg args: Any): String =
        "plural:$id:$count${args.joinToString(prefix = "(", postfix = ")")}"
}

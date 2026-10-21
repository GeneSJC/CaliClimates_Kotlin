package io.github.genesjc.microclimatefinder.ui.map

import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.local.TerrainRepository
import io.github.genesjc.microclimatefinder.data.local.ElevationHintStore
import io.github.genesjc.microclimatefinder.data.remote.LiveObservationCache
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.testLiveReadings
import io.github.genesjc.microclimatefinder.testStation
import io.github.genesjc.microclimatefinder.testutil.FakeHintStore
import io.github.genesjc.microclimatefinder.testutil.FakeObservationSource
import io.github.genesjc.microclimatefinder.testutil.FakeStationSource
import io.github.genesjc.microclimatefinder.testutil.FakeTerrainSource
import io.github.genesjc.microclimatefinder.testutil.MainDispatcherRule
import io.github.genesjc.microclimatefinder.testutil.StopKoinRule
import io.github.genesjc.microclimatefinder.testutil.restartKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapViewModelsTest {

    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    private val dataset = StationDatasetInfo(
        regionId = "a", fileName = "a.txt", title = "North", subtitle = "Sample",
        stationCount = 2, minElevationMeters = 0, maxElevationMeters = 100
    )
    private val stations = listOf(testStation(id = "K1", name = "One"), testStation(id = "K2", name = "Two"))
    private val terrain = FakeTerrainSource()
    private val cardFetches = mutableListOf<String>()
    private val observations = FakeObservationSource()
    private val hints = FakeHintStore()

    private fun register() = restartKoin {
        single { StationRepository(FakeStationSource(stations)) }
        single { TerrainRepository(terrain, Dispatchers.Unconfined) }
        single { testLiveReadings(CoroutineScope(Dispatchers.Unconfined), fetched = cardFetches) }
        single { LiveObservationCache(observations) }
        single<ElevationHintStore> { hints }
    }

    private fun topography(): StationMapViewModel {
        register()
        return StationMapViewModel()
    }

    private fun elevation(): ElevationViewModel {
        register()
        return ElevationViewModel()
    }

    @Test
    fun providesTheStationsAndTheLabelName() {
        val vm = topography()
        assertEquals(stations, vm.stationsFor(dataset))
        assertEquals("One", vm.cityName(stations[0]))
    }

    @Test
    fun loadsEachRegionsTerrainOnlyOnce() {
        val vm = topography()
        assertSame(vm.terrain("a"), vm.terrain("a"))
        vm.terrain("b")
        assertEquals(listOf("a", "b"), terrain.terrainLoads)
    }

    @Test
    fun aCardsReadingStartsAsAFetchAndSkipsStationsThatCannotReport() {
        val vm = topography()
        assertEquals(ObservationResult.Failed, vm.reading(stations[0]).value)
        assertEquals(listOf("K1"), cardFetches)
        assertEquals(ObservationResult.NoReading, vm.reading(testStation(id = "KNO", reportsTemp = false)).value)
        assertEquals(listOf("K1"), cardFetches)
    }

    @Test
    fun keepsOnlyTheLatestRegionsTerrain() {
        val vm = topography()
        vm.terrain("a")
        vm.terrain("b")
        vm.terrain("a")
        assertEquals(listOf("a", "b", "a"), terrain.terrainLoads)
    }

    @Test
    fun elevationLoadsASkylinePerRegionOnce() {
        val vm = elevation()
        assertEquals(1, vm.skyline("a").value?.size)
        vm.skyline("a")
        assertEquals(listOf("a"), terrain.skylineLoads)
    }

    @Test
    fun aStationThatCannotReportIsNotFetched() = runTest {
        val vm = elevation()
        val silent = testStation(id = "KNO", reportsTemp = false)
        assertEquals(ObservationResult.NoReading, vm.latestReading(silent))
        assertTrue(observations.fetched.isEmpty())
        assertEquals(ObservationResult.Failed, vm.latestReading(stations[0]))
        assertEquals(listOf("K1"), observations.fetched)
        assertEquals(ObservationResult.Failed, vm.knownReading("K1"))
        assertNull(vm.knownReading("KOTHER"))
    }

    @Test
    fun theHintShowsUntilTheLineIsTapped() {
        val vm = elevation()
        assertTrue(vm.startVisit())
        assertEquals(1, hints.launches)
        vm.lineTapped()
        assertFalse(vm.startVisit())
    }
}

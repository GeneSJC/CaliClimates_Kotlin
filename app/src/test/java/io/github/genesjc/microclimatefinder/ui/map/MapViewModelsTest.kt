package io.github.genesjc.microclimatefinder.ui.map

import io.github.genesjc.microclimatefinder.data.local.ElevationHintStore
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.testLiveReadings
import io.github.genesjc.microclimatefinder.testStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelsTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dataset = StationDatasetInfo(
        regionId = "a", fileName = "a.txt", title = "North", subtitle = "Sample",
        stationCount = 2, minElevationMeters = 0, maxElevationMeters = 100
    )
    private val stations = listOf(testStation(id = "K1", name = "One"), testStation(id = "K2", name = "Two"))
    private val terrainLoads = mutableListOf<String>()
    private val skylineLoads = mutableListOf<String>()
    private val fetched = mutableListOf<String>()

    private class FakeHintStore(var launches: Int = 0, var tapped: Boolean = false) : ElevationHintStore {
        override fun countLaunch() {
            launches++
        }

        override fun shouldShow() = !tapped && launches <= 3
        override fun markLineTapped() {
            tapped = true
        }
    }

    private val hints = FakeHintStore()

    private fun topography() = StationMapViewModel(
        stationsOf = { stations },
        loadTerrain = { regionId -> terrainLoads += regionId; null },
        cityNameOf = { it.name.uppercase() },
        readings = testLiveReadings(CoroutineScope(dispatcher), fetched = fetched),
        ioDispatcher = dispatcher
    )

    private fun elevation() = ElevationViewModel(
        stationsOf = { stations },
        loadTerrain = { regionId -> terrainLoads += regionId; null },
        cityNameOf = { it.name.uppercase() },
        readings = testLiveReadings(CoroutineScope(dispatcher), fetched = fetched),
        loadSkyline = { regionId -> skylineLoads += regionId; listOf(floatArrayOf(0f, 1f)) },
        peekReading = { null },
        fetchReading = { id -> fetched += id; ObservationResult.Failed },
        hintStore = hints,
        io = dispatcher
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun providesTheStationsAndTheLabelName() {
        val vm = topography()
        assertEquals(stations, vm.stationsFor(dataset))
        assertEquals("ONE", vm.cityName(stations[0]))
    }

    @Test
    fun loadsEachRegionsTerrainOnlyOnce() {
        val vm = topography()
        assertSame(vm.terrain("a"), vm.terrain("a"))
        vm.terrain("b")
        assertEquals(listOf("a", "b"), terrainLoads)
    }

    @Test
    fun aCardsReadingStartsAsAFetchAndSkipsStationsThatCannotReport() {
        val vm = topography()
        assertEquals(ObservationResult.Failed, vm.reading(stations[0]).value)
        assertEquals(listOf("K1"), fetched)
        assertEquals(ObservationResult.NoReading, vm.reading(testStation(id = "KNO", reportsTemp = false)).value)
        assertEquals(listOf("K1"), fetched)
    }

    @Test
    fun keepsOnlyTheLatestRegionsTerrain() {
        val vm = topography()
        vm.terrain("a")
        vm.terrain("b")
        vm.terrain("a")
        assertEquals(listOf("a", "b", "a"), terrainLoads)
    }

    @Test
    fun elevationLoadsASkylinePerRegionOnce() {
        val vm = elevation()
        assertEquals(1, vm.skyline("a").value?.size)
        vm.skyline("a")
        assertEquals(listOf("a"), skylineLoads)
    }

    @Test
    fun aStationThatCannotReportIsNotFetched() = runTest {
        val vm = elevation()
        val silent = testStation(id = "KNO", reportsTemp = false)
        assertEquals(ObservationResult.NoReading, vm.reading(silent))
        assertTrue(fetched.isEmpty())
        assertEquals(ObservationResult.Failed, vm.reading(stations[0]))
        assertEquals(listOf("K1"), fetched)
        assertNull(vm.knownReading("K1"))
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

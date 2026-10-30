package io.github.genesjc.climatefinder.ui.map

import io.github.genesjc.climatefinder.data.local.DatasetKind
import io.github.genesjc.climatefinder.data.local.StationDatasetInfo
import io.github.genesjc.climatefinder.data.local.StationRepository
import io.github.genesjc.climatefinder.data.local.StationTowns
import io.github.genesjc.climatefinder.domain.model.StateUsaCA
import io.github.genesjc.climatefinder.data.local.TerrainRepository
import io.github.genesjc.climatefinder.data.local.AppSettings
import io.github.genesjc.climatefinder.data.remote.ObservationResult
import io.github.genesjc.climatefinder.domain.BandAxis
import io.github.genesjc.climatefinder.testLiveReadings
import io.github.genesjc.climatefinder.testStation
import io.github.genesjc.climatefinder.testutil.FakeAppSettings
import io.github.genesjc.climatefinder.testutil.FakeObservationSource
import io.github.genesjc.climatefinder.testutil.FakeStationSource
import io.github.genesjc.climatefinder.testutil.FakeTerrainSource
import io.github.genesjc.climatefinder.testutil.MainDispatcherRule
import io.github.genesjc.climatefinder.testutil.StopKoinRule
import io.github.genesjc.climatefinder.testutil.restartKoin
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
        regionId = "a", fileName = "a.txt", kind = DatasetKind.BUNDLED_PACK,
        stationCount = 2, minElevationMeters = 0, maxElevationMeters = 100
    )
    private val stations = listOf(testStation(id = "K1", name = "One"), testStation(id = "K2", name = "Two"))
    private val terrain = FakeTerrainSource()
    private val observations = FakeObservationSource()
    private val hints = FakeAppSettings()

    private fun register() = restartKoin {
        single { StationRepository(FakeStationSource(stations), Dispatchers.Unconfined) }
        single { StationTowns(StateUsaCA.stationTowns) }
        single { TerrainRepository(terrain, Dispatchers.Unconfined) }
        single { testLiveReadings(CoroutineScope(Dispatchers.Unconfined), observations) }
        single<AppSettings> { hints }
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
    fun theElevationBandsStartFromTheSavedAngle() {
        assertEquals(0, elevation().bandAngle.value)
        assertEquals(BandAxis.NORTH_SOUTH, elevation().bandAxis.value)
        hints.angle = 90
        assertEquals(90, elevation().bandAngle.value)
        assertEquals(BandAxis.WEST_EAST, elevation().bandAxis.value)
    }

    @Test
    fun settingTheBandAngleSavesItAndMovesTheNearestAxis() {
        val vm = elevation()
        vm.setBandAngle(30)
        assertEquals(30, vm.bandAngle.value)
        assertEquals(30, hints.angle)
        assertEquals(BandAxis.NORTH_SOUTH, vm.bandAxis.value)
        vm.setBandAngle(45)
        assertEquals(BandAxis.WEST_EAST, vm.bandAxis.value)
        vm.setBandAngle(500)
        assertEquals(90, vm.bandAngle.value)
    }

    @Test
    fun togglingTheBandAxisGoesToTheFarEndAndSavesIt() {
        val vm = elevation()
        vm.toggleBandAxis()
        assertEquals(90, vm.bandAngle.value)
        assertEquals(BandAxis.WEST_EAST, vm.bandAxis.value)
        assertEquals(90, hints.angle)
        vm.toggleBandAxis()
        assertEquals(0, vm.bandAngle.value)
        assertEquals(BandAxis.NORTH_SOUTH, vm.bandAxis.value)
        assertEquals(0, hints.angle)
    }

    @Test
    fun togglingFromAHalfwayAngleGoesToTheNearerEndsFarSide() {
        val vm = elevation()
        vm.setBandAngle(20)
        vm.toggleBandAxis()
        assertEquals(90, vm.bandAngle.value)
        vm.setBandAngle(70)
        vm.toggleBandAxis()
        assertEquals(0, vm.bandAngle.value)
    }

    @Test
    fun togglingTheBandAxisDoesNotTouchTheLayersSetting() {
        val vm = elevation()
        vm.toggleBandAxis()
        assertFalse(vm.layers.value)
        assertFalse(hints.layers)
    }

    @Test
    fun providesTheStationsAndTheLabelName() {
        val vm = topography()
        assertEquals(stations, vm.stations(dataset).value)
        assertSame(vm.stations(dataset), vm.stations(dataset))
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
        assertEquals(listOf("K1"), observations.fetched)
        assertEquals(ObservationResult.NoReading, vm.reading(testStation(id = "KNO", reportsTemp = false)).value)
        assertEquals(listOf("K1"), observations.fetched)
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

package io.github.genesjc.microclimatefinder.ui.home

import io.github.genesjc.microclimatefinder.data.local.MapZoomStore
import io.github.genesjc.microclimatefinder.data.local.RegionDatasets
import io.github.genesjc.microclimatefinder.data.local.SelectedRegionStore
import io.github.genesjc.microclimatefinder.data.local.SelectedStationStore
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.testLiveReadings
import io.github.genesjc.microclimatefinder.testutil.FakeDatasets
import io.github.genesjc.microclimatefinder.testutil.FakeObservationSource
import io.github.genesjc.microclimatefinder.testutil.FakeRegionStore
import io.github.genesjc.microclimatefinder.testutil.FakeStationSource
import io.github.genesjc.microclimatefinder.testutil.FakeStationStore
import io.github.genesjc.microclimatefinder.testutil.FakeZoomStore
import io.github.genesjc.microclimatefinder.testutil.MainDispatcherRule
import io.github.genesjc.microclimatefinder.testutil.StopKoinRule
import io.github.genesjc.microclimatefinder.testutil.restartKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    private val store = FakeRegionStore("a")
    private val zoomStore = FakeZoomStore()
    private val stationStore = FakeStationStore()
    private val cancelledFor = mutableListOf<String>()
    private val observations = FakeObservationSource(onFetch = { id ->
        try {
            awaitCancellation()
        } finally {
            cancelledFor += id
        }
    })

    private val repository = StationRepository(FakeStationSource(), Dispatchers.Unconfined)

    private fun viewModel(): HomeViewModel {
        restartKoin {
            single<SelectedRegionStore> { store }
            single<SelectedStationStore> { stationStore }
            single<MapZoomStore> { zoomStore }
            single<RegionDatasets> { FakeDatasets() }
            single { repository }
            single { testLiveReadings(CoroutineScope(Dispatchers.Unconfined), observations) }
        }
        return HomeViewModel()
    }

    @Test
    fun startsOnTheSavedRegion() {
        val state = viewModel().uiState.value
        assertEquals("a", state.selectedRegionId)
        assertNull(state.selectedStationId)
    }

    @Test
    fun choosingARegionSavesItAndClearsTheSelectedStation() {
        val vm = viewModel()
        vm.selectStation("KXYZ")
        vm.selectRegion("b")
        assertEquals("b", store.saved)
        assertEquals("b", vm.uiState.value.selectedRegionId)
        assertNull(vm.uiState.value.selectedStationId)
    }

    @Test
    fun sharedMapSettingsAreKeptInOneState() {
        val vm = viewModel()
        vm.setTab(1)
        vm.selectStation("KXYZ")
        vm.setPinScale(4f)
        vm.setSliderShown(false)
        vm.setInfoOpen(true)
        val state = vm.uiState.value
        assertEquals(1, state.tab)
        assertEquals("KXYZ", state.selectedStationId)
        assertEquals(4f, state.pinScale)
        assertEquals(false, state.sliderShown)
        assertTrue(state.infoOpen)
    }

    @Test
    fun reopensOnTheLastSelectedStationOfTheSavedRegion() {
        stationStore.save("a", "KXYZ")
        assertEquals("KXYZ", viewModel().uiState.value.selectedStationId)
    }

    @Test
    fun ignoresASavedStationFromAnotherRegion() {
        stationStore.save("b", "KXYZ")
        assertNull(viewModel().uiState.value.selectedStationId)
    }

    @Test
    fun rememberingFollowsSelectionAndForgetsOnRegionChange() {
        val vm = viewModel()
        vm.selectStation("KXYZ")
        assertEquals("a", stationStore.region)
        assertEquals("KXYZ", stationStore.station)
        vm.selectRegion("b")
        assertEquals("b", stationStore.region)
        assertNull(stationStore.station)
    }

    @Test
    fun reopensAtTheSavedZoomOfTheSavedRegion() {
        zoomStore.save("a", 3f)
        assertEquals(3f, viewModel().mapZoom)
    }

    @Test
    fun ignoresASavedZoomFromAnotherRegion() {
        zoomStore.save("b", 3f)
        assertEquals(1f, viewModel().mapZoom)
    }

    @Test
    fun savesTheZoomOnceItHasSettledAndResetsItOnRegionChange() {
        val vm = viewModel()
        vm.setMapZoom(2f)
        vm.setMapZoom(4f)
        assertEquals(4f, vm.mapZoom)
        assertEquals(1f, zoomStore.zoom)
        main.dispatcher.scheduler.advanceTimeBy(600)
        assertEquals("a", zoomStore.region)
        assertEquals(4f, zoomStore.zoom)
        vm.selectRegion("b")
        assertEquals(1f, vm.mapZoom)
        assertEquals("b", zoomStore.region)
        assertEquals(1f, zoomStore.zoom)
    }

    @Test
    fun theRegionAloneIsAvailableForWhatSitsAboveHome() {
        val vm = viewModel()
        assertEquals("a", vm.selectedRegionId.value)
        vm.selectRegion("b")
        assertEquals("b", vm.selectedRegionId.value)
    }

    @Test
    fun aRegionChangeSendsOneEventAndChoosingTheSameRegionSendsNone() = runTest {
        val vm = viewModel()
        vm.selectRegion("a")
        assertNull(withTimeoutOrNull(1000) { vm.events.first() })
        vm.selectRegion("b")
        assertEquals(HomeEvent.RegionChanged, vm.events.first())
    }

    @Test
    fun providesTheStationsOfARegionForHomesLists() = runBlocking {
        val vm = viewModel()
        assertEquals(emptyList<String>(), vm.stations("b").map { it.id })   // never reads: nothing is parsed on the caller
        repository.load(FakeDatasets().datasetFor("b"))
        assertEquals(listOf("b"), vm.stations("b").map { it.id })
    }

    @Test
    fun answersForTheDatasetsOfRegions() {
        val vm = viewModel()
        assertEquals("b", vm.datasetFor("b")?.regionId)
        assertEquals("default", vm.defaultDataset().regionId)
        assertEquals(5, vm.fullDataRegionCount())
        assertEquals(6, vm.totalRegionCount())
    }

    @Test
    fun fetchesTheNewRegionAndCancelsThePreviousFetch() {
        val vm = viewModel()
        assertEquals(listOf("a"), observations.fetched)
        vm.selectRegion("b")
        assertEquals(listOf("a", "b"), observations.fetched)
        assertEquals(listOf("a"), cancelledFor)
    }
}

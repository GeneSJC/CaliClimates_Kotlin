package io.github.genesjc.microclimatefinder.ui.home

import io.github.genesjc.microclimatefinder.data.local.MapZoomStore
import io.github.genesjc.microclimatefinder.data.local.SelectedRegionStore
import io.github.genesjc.microclimatefinder.data.local.SelectedStationStore
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.testStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeRegionStore(var saved: String) : SelectedRegionStore {
        override fun load() = saved
        override fun save(regionId: String) {
            saved = regionId
        }
    }

    private class FakeStationStore(var region: String? = null, var station: String? = null) : SelectedStationStore {
        override fun load(regionId: String) = station?.takeIf { region == regionId }
        override fun save(regionId: String, stationId: String?) {
            region = regionId
            station = stationId
        }
    }

    private class FakeZoomStore(var region: String? = null, var zoom: Float = 1f) : MapZoomStore {
        override fun load(regionId: String) = if (region == regionId) zoom else 1f
        override fun save(regionId: String, zoom: Float) {
            region = regionId
            this.zoom = zoom
        }
    }

    private val store = FakeRegionStore("a")
    private val zoomStore = FakeZoomStore()
    private val stationStore = FakeStationStore()
    private val fetchedFor = mutableListOf<String>()
    private val cancelledFor = mutableListOf<String>()

    private fun viewModel() = HomeViewModel(
        regionStore = store,
        stationStore = stationStore,
        zoomStore = zoomStore,
        stationsForRegion = { regionId -> listOf(testStation(id = regionId)) },
        prefetch = { stations: List<MicroclimateStation> ->
            val regionId = stations.first().id
            fetchedFor += regionId
            try {
                awaitCancellation()
            } finally {
                cancelledFor += regionId
            }
        },
        ioDispatcher = dispatcher
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

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
        dispatcher.scheduler.advanceTimeBy(600)
        assertEquals("a", zoomStore.region)
        assertEquals(4f, zoomStore.zoom)
        vm.selectRegion("b")
        assertEquals(1f, vm.mapZoom)
        assertEquals("b", zoomStore.region)
        assertEquals(1f, zoomStore.zoom)
    }

    @Test
    fun fetchesTheNewRegionAndCancelsThePreviousFetch() {
        val vm = viewModel()
        assertEquals(listOf("a"), fetchedFor)
        vm.selectRegion("b")
        assertEquals(listOf("a", "b"), fetchedFor)
        assertEquals(listOf("a"), cancelledFor)
    }
}

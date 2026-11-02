package io.github.genesjc.climatefinder.ui.home

import io.github.genesjc.climatefinder.data.local.RegionDatasetCatalog
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo
import io.github.genesjc.climatefinder.data.local.StationRepository
import io.github.genesjc.climatefinder.testLiveReadingsRepository
import io.github.genesjc.climatefinder.config.StateUsaCA
import io.github.genesjc.climatefinder.config.StateUsaDefinition
import io.github.genesjc.climatefinder.testutil.FakeDatasets
import io.github.genesjc.climatefinder.testutil.FakeObservationSource
import io.github.genesjc.climatefinder.data.local.AppSettings
import io.github.genesjc.climatefinder.testutil.FakeAppSettings
import io.github.genesjc.climatefinder.testutil.FakeStationSource
import io.github.genesjc.climatefinder.testutil.MainDispatcherRule
import io.github.genesjc.climatefinder.testutil.StopKoinRule
import io.github.genesjc.climatefinder.testutil.restartKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import kotlinx.coroutines.CompletableDeferred
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

    private val settings = FakeAppSettings(region = "a")
    private val cancelledFor = mutableListOf<String>()
    private val observations = FakeObservationSource(onFetch = { id ->
        try {
            awaitCancellation()
        } finally {
            cancelledFor += id
        }
    })

    private val repository = StationRepository(FakeStationSource(), Dispatchers.Unconfined)

    private fun viewModel(datasets: RegionDatasetCatalog = FakeDatasets()): HomeViewModel {
        restartKoin {
            single<AppSettings> { settings }
            single<RegionDatasetCatalog> { datasets }
            single<StateUsaDefinition> { StateUsaCA }
            single { repository }
            single { testLiveReadingsRepository(CoroutineScope(Dispatchers.Unconfined), observations) }
        }
        return HomeViewModel()
    }

    /** Datasets where reading the stations of the region called "slow" waits until the test lets it finish. */
    private class GatedDatasets(private val gate: CompletableDeferred<Unit>) : RegionDatasetCatalog by FakeDatasets() {
        override suspend fun stationsFor(regionId: String): List<MicroclimateStation> {
            if (regionId == "slow") gate.await()
            return FakeDatasets().stationsFor(regionId)
        }
    }

    /** Datasets where no region has any data. */
    private class NoDataDatasets : RegionDatasetCatalog by FakeDatasets() {
        override suspend fun datasetFor(regionId: String): StationDatasetInfo? = null
        override fun cachedDatasetFor(regionId: String): StationDatasetInfo? = null
        override suspend fun stationsFor(regionId: String): List<MicroclimateStation> = emptyList()
    }

    @Test
    fun aRegionIsReadBeforeTheScreenSwitchesToIt() {
        val gate = CompletableDeferred<Unit>()
        val vm = viewModel(GatedDatasets(gate))
        vm.selectRegion("slow")
        // Still reading it: nothing has changed yet, so the screen is not asked to read it on the main thread
        assertEquals("a", vm.uiState.value.selectedRegionId)
        assertEquals("a", settings.region)
        gate.complete(Unit)
        assertEquals("slow", vm.uiState.value.selectedRegionId)
        assertEquals("slow", settings.region)
    }

    @Test
    fun choosingAnotherRegionWhileOneIsLoadingReplacesIt() {
        val gate = CompletableDeferred<Unit>()
        val vm = viewModel(GatedDatasets(gate))
        vm.selectRegion("slow")
        vm.selectRegion("b")
        assertEquals("b", vm.uiState.value.selectedRegionId)
        gate.complete(Unit)
        // The first choice was replaced, so it does not switch the screen back
        assertEquals("b", vm.uiState.value.selectedRegionId)
        assertEquals("b", settings.region)
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
        assertEquals("b", settings.region)
        assertEquals("b", vm.uiState.value.selectedRegionId)
        assertNull(vm.uiState.value.selectedStationId)
    }

    @Test
    fun theChosenViewIsSavedAndTheNextLaunchStartsOnIt() {
        val vm = viewModel()
        assertEquals(0, vm.uiState.value.tab)
        vm.setTab(1)
        assertEquals(1, settings.tab)
        assertEquals(1, viewModel().uiState.value.tab)
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
        settings.saveSelectedStationId("a", "KXYZ")
        assertEquals("KXYZ", viewModel().uiState.value.selectedStationId)
    }

    @Test
    fun ignoresASavedStationFromAnotherRegion() {
        settings.saveSelectedStationId("b", "KXYZ")
        assertNull(viewModel().uiState.value.selectedStationId)
    }

    @Test
    fun rememberingFollowsSelectionAndForgetsOnRegionChange() {
        val vm = viewModel()
        vm.selectStation("KXYZ")
        assertEquals("a", settings.stationRegion)
        assertEquals("KXYZ", settings.station)
        vm.selectRegion("b")
        assertEquals("b", settings.stationRegion)
        assertNull(settings.station)
    }

    @Test
    fun reopensAtTheSavedZoomOfTheSavedRegion() {
        settings.saveMapZoom("a", 3f)
        assertEquals(3f, viewModel().mapZoom)
    }

    @Test
    fun ignoresASavedZoomFromAnotherRegion() {
        settings.saveMapZoom("b", 3f)
        assertEquals(1f, viewModel().mapZoom)
    }

    @Test
    fun savesTheZoomOnceItHasSettledAndResetsItOnRegionChange() {
        val vm = viewModel()
        vm.setMapZoom(2f)
        vm.setMapZoom(4f)
        assertEquals(4f, vm.mapZoom)
        assertEquals(1f, settings.zoom)
        main.dispatcher.scheduler.advanceTimeBy(600)
        assertEquals("a", settings.zoomRegion)
        assertEquals(4f, settings.zoom)
        vm.selectRegion("b")
        assertEquals(1f, vm.mapZoom)
        assertEquals("b", settings.zoomRegion)
        assertEquals(1f, settings.zoom)
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
        repository.load(FakeDatasets().cachedDatasetFor("b"))
        assertEquals(listOf("b"), vm.stations("b").map { it.id })
    }

    @Test
    fun answersForTheDatasetsOfRegions() = runBlocking {
        val vm = viewModel()
        assertEquals("default", vm.defaultDataset().regionId)
        assertEquals("empty", vm.emptyDefaultDataset().regionId)
        assertEquals(5, vm.fullDataRegionCount())
        assertEquals(6, vm.totalRegionCount())
    }

    @Test
    fun theSavedRegionsDatasetIsInTheStateOnceItsStationsAreRead() {
        val vm = viewModel()
        assertEquals(false, vm.uiState.value.loading)
        assertEquals("a", vm.uiState.value.regionDataset?.regionId)
    }

    @Test
    fun homeIsLoadingUntilTheSavedRegionsStationsAreRead() {
        val gate = CompletableDeferred<Unit>()
        settings.saveSelectedRegionId("slow")
        val vm = viewModel(GatedDatasets(gate))
        assertEquals(true, vm.uiState.value.loading)
        assertNull(vm.uiState.value.regionDataset)
        gate.complete(Unit)
        assertEquals(false, vm.uiState.value.loading)
        assertEquals("slow", vm.uiState.value.regionDataset?.regionId)
    }

    @Test
    fun choosingARegionPutsItsDatasetInTheState() {
        val vm = viewModel()
        vm.selectRegion("b")
        assertEquals("b", vm.uiState.value.regionDataset?.regionId)
    }

    @Test
    fun aRegionWithNoDataLeavesTheDatasetEmptyOnceLoaded() {
        val vm = viewModel(NoDataDatasets())
        assertEquals(false, vm.uiState.value.loading)
        assertNull(vm.uiState.value.regionDataset)
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

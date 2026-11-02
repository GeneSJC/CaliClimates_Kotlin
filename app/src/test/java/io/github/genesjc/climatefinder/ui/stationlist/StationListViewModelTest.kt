package io.github.genesjc.climatefinder.ui.stationlist

import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.model.DatasetKind
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo
import io.github.genesjc.climatefinder.data.local.StationRepository
import io.github.genesjc.climatefinder.config.StateUsaCA
import io.github.genesjc.climatefinder.config.StateUsaDefinition
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.testLiveReadingsRepository
import io.github.genesjc.climatefinder.testStation
import io.github.genesjc.climatefinder.testutil.FakeStationSource
import io.github.genesjc.climatefinder.testutil.FakeStringResolver
import io.github.genesjc.climatefinder.testutil.MainDispatcherRule
import io.github.genesjc.climatefinder.testutil.StopKoinRule
import io.github.genesjc.climatefinder.testutil.restartKoin
import io.github.genesjc.climatefinder.ui.common.StringResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StationListViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    private val dataset = StationDatasetInfo(
        regionId = "a", fileName = "a.txt", kind = DatasetKind.BUNDLED_PACK,
        stationCount = 3, minElevationMeters = 0, maxElevationMeters = 100
    )
    private val stations = listOf(testStation(id = "K1"), testStation(id = "K2"), testStation(id = "K3"))

    // Builds the expected text with the same call: the tests check which resource was chosen, not the English
    private val s = FakeStringResolver

    private fun viewModel(list: List<MicroclimateStation> = stations): StationListViewModel {
        restartKoin {
            single { StationRepository(FakeStationSource(list), Dispatchers.Unconfined) }
            single { testLiveReadingsRepository(CoroutineScope(Dispatchers.Unconfined)) }
            single<StringResolver> { FakeStringResolver }
            single<StateUsaDefinition> { StateUsaCA }
        }
        return StationListViewModel()
    }

    @Test
    fun marksTheStationSelectedOnHomeAndOpensThere() {
        val state = viewModel().stateFor(dataset, stations, "K2")
        assertEquals("K2", state.activeStationId)
        assertEquals(1, state.initialIndex)
        assertEquals(s(R.string.station_list_showing_on_map), state.selectedLabel)
        assertNull(state.emptyMessage)
    }

    @Test
    fun marksTheFirstStationWhenNothingValidIsSelected() {
        assertEquals("K1", viewModel().stateFor(dataset, stations, null).activeStationId)
        val state = viewModel().stateFor(dataset, stations, "KGONE")
        assertEquals("K1", state.activeStationId)
        assertEquals(0, state.initialIndex)
    }

    @Test
    fun carriesTheDatasetHeader() {
        val state = viewModel().stateFor(dataset, stations, null)
        // "a" is not one of the regions, so its id stands in for the name
        assertEquals("a", state.title)
        assertEquals(
            s(R.string.dataset_subtitle, s(R.string.dataset_kind_bundled), s.plural(R.plurals.station_count, 3, 3)),
            state.subtitle
        )
        assertEquals(3, state.stations.size)
    }

    @Test
    fun namesTheRegionWithItsResource() {
        val bay = dataset.copy(regionId = StateUsaCA.BAY_AREA_SACRAMENTO.id)
        assertEquals(s(StateUsaCA.BAY_AREA_SACRAMENTO.nameRes), viewModel().stateFor(bay, stations, null).title)
    }

    @Test
    fun saysWhereTheStationsCameFromOrThatThereAreNone() {
        val csv = dataset.copy(kind = DatasetKind.CSV_OVERRIDE)
        assertEquals(
            s(R.string.dataset_subtitle, s(R.string.dataset_kind_csv), s.plural(R.plurals.station_count, 3, 3)),
            viewModel().stateFor(csv, stations, null).subtitle
        )
        val none = dataset.copy(kind = DatasetKind.NONE)
        assertEquals(s(R.string.dataset_kind_none), viewModel().stateFor(none, emptyList(), null).subtitle)
    }

    @Test
    fun explainsAnEmptyDataset() {
        val state = viewModel(emptyList()).stateFor(dataset, emptyList(), "K1")
        assertNull(state.activeStationId)
        assertEquals(0, state.initialIndex)
        assertEquals(s(R.string.station_list_empty, "a.txt"), state.emptyMessage)
    }

    @Test
    fun readsTheStationsOffTheCallerAndSharesTheFlow() {
        val vm = viewModel()
        val flow = vm.stations(dataset)
        assertEquals(stations, flow.value)
        assertTrue(flow === vm.stations(dataset))
    }

    @Test
    fun showsALoadingStateUntilTheStationsAreRead() {
        val state = viewModel().stateFor(dataset, null, "K1")
        assertTrue(state.isLoading)
        assertNull(state.emptyMessage)
        assertEquals("a", state.title)
        assertTrue(state.stations.isEmpty())
    }
}
